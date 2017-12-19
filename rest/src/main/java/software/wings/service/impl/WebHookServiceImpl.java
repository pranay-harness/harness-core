package software.wings.service.impl;

import static software.wings.utils.Misc.isNullOrEmpty;

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
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WebHookService;
import software.wings.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
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
      Trigger trigger = triggerService.getTriggerByWebhookToken(token);
      if (trigger == null) {
        return WebHookResponse.builder().error("Trigger not associated to the given token").build();
      }
      WebHookTriggerCondition webhookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
      Map<String, String> webhookParameters = webhookTriggerCondition.getParameters();
      Map<String, String> resolvedParameters = new HashMap<>();
      DocumentContext ctx = JsonUtils.parseJson(webhookEventPayload);
      if (webhookParameters != null) {
        webhookParameters.keySet().forEach(s -> {
          String param = webhookParameters.get(s);
          String paramValue = null;
          try {
            paramValue = JsonUtils.jsonPath(ctx, param);
          } catch (Exception e) {
            logger.warn("Failed to resolve the param {} in Json {}", param, webhookEventPayload);
            if (!isNullOrEmpty(paramValue)) {
              resolvedParameters.put(param, paramValue);
            }
          }
        });
      }
      WorkflowExecution workflowExecution = triggerService.triggerExecutionByWebHook(trigger, resolvedParameters);
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
