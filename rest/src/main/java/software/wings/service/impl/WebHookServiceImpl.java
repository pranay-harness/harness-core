package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import com.jayway.jsonpath.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecutionStatusResponse;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.expression.ExpressionEvaluator;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WebHookService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class WebHookServiceImpl implements WebHookService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private TriggerService triggerService;
  @Inject private AppService appService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private MainConfiguration configuration;

  private static final Logger logger = LoggerFactory.getLogger(WebHookServiceImpl.class);

  @Override
  public WorkflowExecutionStatusResponse getWorkflowExecutionStatus(
      final String statusToken, final String appId, final String workflowExecutionId) {
    final WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    if (workflowExecution != null) {
      return WorkflowExecutionStatusResponse.builder().status(workflowExecution.getStatus().name()).build();
    } else {
      throw new InvalidRequestException(String.format("Workflow with id: [%s], appId: [%s], token: [%s] not found",
                                            workflowExecutionId, appId, statusToken),
          ReportTarget.USER);
    }
  }

  private String getBaseUrl() {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  private String getUiUrl(String accountId, String appId, String envId, String workflowExecutionId) {
    return String.format("%s#/account/%s/app/%s/env/%s/executions/%s/details", getBaseUrl(), accountId, appId, envId,
        workflowExecutionId);
  }

  private String getRestUrl(String appId, String workflowExecutionId) {
    return String.format("%sapi/webhooks/%s/status?appId=%s&workflowExecutionId=%s", getBaseUrl(),
        CryptoUtil.secureRandAlphaNumString(40), appId, workflowExecutionId);
  }

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
        if (isBlank(webHookRequest.getBuildNumber()) && isBlank(webHookRequest.getDockerImageTag())) {
          artifact = artifactService.fetchLatestArtifactForArtifactStream(
              appId, artifactStreamId, artifactStream.getSourceName());
        } else {
          String requestBuildNumber = isBlank(webHookRequest.getBuildNumber()) ? webHookRequest.getDockerImageTag()
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
          .restUrl(getRestUrl(appId, workflowExecution.getUuid()))
          .uiUrl(getUiUrl(app.getAccountId(), appId, workflowExecution.getEnvId(), workflowExecution.getUuid()))
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
      Map<String, String> webhookParameters = webhookTriggerCondition.getParameters();
      Map<String, String> resolvedParameters = new HashMap<>();
      DocumentContext ctx = JsonUtils.parseJson(webhookEventPayload);
      if (webhookParameters != null) {
        for (String s : webhookParameters.keySet()) {
          String param = webhookParameters.get(s);
          Object paramValue = null;
          try {
            Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(param);
            if (matcher.matches()) {
              String paramVariable = matcher.group(0).substring(2, matcher.group(0).length() - 1);
              logger.info("Param Variable {}", paramVariable);
              paramValue = JsonUtils.jsonPath(ctx, paramVariable);
            } else {
              logger.info("Not variable {}", param);
              paramValue = JsonUtils.jsonPath(ctx, param);
            }
          } catch (Exception e) {
            logger.warn("Failed to resolve the param {} in Json {}", param, webhookEventPayload);
          }
          if (paramValue != null) {
            resolvedParameters.put(s, String.valueOf(paramValue));
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
