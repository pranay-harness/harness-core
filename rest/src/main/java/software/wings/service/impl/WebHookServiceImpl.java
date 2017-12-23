package software.wings.service.impl;

import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.utils.Misc.isNullOrEmpty;

import com.google.inject.Inject;

import com.jayway.jsonpath.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WebHookService;
import software.wings.utils.ExpressionEvaluator;
import software.wings.utils.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class WebHookServiceImpl implements WebHookService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private TriggerService triggerService;
  @Inject private AppService appService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public WebHookResponse execute(String token, WebHookRequest webHookRequest) {
    try {
      String appId = webHookRequest.getApplication();
      Application app = appService.get(appId);
      if (app == null) {
        return WebHookResponse.builder().error("Application does not exist").build();
      }
      String artifactStreamId = webHookRequest.getArtifactSource();
      WorkflowExecution workflowExecution;
      if (artifactStreamId != null) {
        // TODO: For backward compatible purpose
        ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
        if (artifactStream == null) {
          return WebHookResponse.builder().error("Invalid request payload. ArtifactStream does not exists").build();
        }
        Artifact artifact;
        if (isNullOrEmpty(webHookRequest.getBuildNumber()) && isNullOrEmpty(webHookRequest.getDockerImageTag())) {
          artifact = artifactService.fetchLatestArtifactForArtifactStream(
              appId, artifactStreamId, artifactStream.getSourceName());
        } else {
          String requestBuildNumber = isNullOrEmpty(webHookRequest.getBuildNumber())
              ? webHookRequest.getDockerImageTag()
              : webHookRequest.getBuildNumber();
          artifact = artifactService.getArtifactByBuildNumber(appId, artifactStreamId, requestBuildNumber);
          if (artifact == null) {
            // do collection and then run
            logger.warn("Artifact not found for webhook request " + webHookRequest);
          }
        }
        workflowExecution =
            triggerService.triggerExecutionByWebHook(appId, token, artifact, webHookRequest.getParameters());
      } else {
        Map<String, String> serviceBuildNumbers = new HashMap<>();
        if (webHookRequest.getArtifacts() != null) {
          for (Map<String, String> artifact : webHookRequest.getArtifacts()) {
            if (artifact.get("service") != null) {
              serviceBuildNumbers.put(artifact.get("service"), artifact.get("buildNumber"));
            }
          }
        }
        workflowExecution =
            triggerService.triggerExecutionByWebHook(appId, token, serviceBuildNumbers, webHookRequest.getParameters());
      }
      return WebHookResponse.builder()
          .requestId(workflowExecution.getUuid())
          .status(workflowExecution.getStatus().name())
          .build();
    } catch (Exception ex) {
      logger.error("WebHook call failed [%s]", token, ex);
      return WebHookResponse.builder().error(ex.getMessage().toLowerCase()).build();
    }
  }

  @Override
  public WebHookResponse executeByEvent(String token, String webhookEventPayload) {
    try {
      logger.info("Received the webhook event payload {}", webhookEventPayload);
      Trigger trigger = triggerService.getTriggerByWebhookToken(token);
      if (trigger == null) {
        return WebHookResponse.builder().error("Trigger not associated to the given token").build();
      }
      WebHookTriggerCondition webhookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
      String pullRequestPrefix = "pullrequest.";
      boolean bitBucketPullRequest = false;
      if (webhookTriggerCondition.getWebhookSource() != null
          && BITBUCKET.equals(webhookTriggerCondition.getWebhookSource())) {
        List<WebhookEventType> eventTypes = webhookTriggerCondition.getEventTypes();
        if (webhookEventPayload.contains("pullrequest") || webhookEventPayload.contains("pullRequest")) {
          if (webhookEventPayload.contains("pullRequest")) {
            pullRequestPrefix = "pullRequest.";
          }
          if (eventTypes.contains(WebhookEventType.PULL_REQUEST)) {
            bitBucketPullRequest = true;
          }
        } else {
          return WebHookResponse.builder().error("Only Pull Request supported for Bit Bucket.").build();
        }
      } else {
        return WebHookResponse.builder().error("Invalid Webhook Source. Only Bit Bucket supported now.").build();
      }
      Map<String, String> webhookParameters = webhookTriggerCondition.getParameters();
      Map<String, String> resolvedParameters = new HashMap<>();
      DocumentContext ctx = JsonUtils.parseJson(webhookEventPayload);
      if (webhookParameters != null) {
        for (String s : webhookParameters.keySet()) {
          String param = webhookParameters.get(s);
          String paramValue = null;
          try {
            Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(param);
            if (matcher.matches()) {
              String paramVariable = matcher.group(0).substring(2, matcher.group(0).length() - 1);
              if (bitBucketPullRequest) {
                if (!paramVariable.startsWith("pullrequest") || !paramVariable.startsWith("pullRequest")) {
                  paramVariable = pullRequestPrefix + paramVariable;
                }
              }
              logger.info("Param Variable {}", paramVariable);
              paramValue = JsonUtils.jsonPath(ctx, paramVariable);
            } else {
              logger.info("Not variable {}", param);
              paramValue = JsonUtils.jsonPath(ctx, param);
            }
          } catch (Exception e) {
            logger.warn("Failed to resolve the param {} in Json {}", param, webhookEventPayload);
          }
          if (!isNullOrEmpty(paramValue)) {
            resolvedParameters.put(s, paramValue);
          } else {
            resolvedParameters.put(s, param);
          }
        }
      }
      logger.info("Triggering pipeline execution");
      WorkflowExecution workflowExecution = triggerService.triggerExecutionByWebHook(trigger, resolvedParameters);
      logger.info("Pipeline execution trigger success");
      return WebHookResponse.builder()
          .requestId(workflowExecution.getUuid())
          .status(workflowExecution.getStatus().name())
          .build();

    } catch (Exception ex) {
      logger.error("WebHook call failed [%s]", token, ex);
      return WebHookResponse.builder().error(ex.getMessage().toLowerCase()).build();
    }
  }
}

// generate requestId and save workflow executionId map
// queue multiple request
// compare queued requests
// wait for artifact to appear
// return response;
// Already running workflow
