package software.wings.service.impl;

import static java.lang.String.format;
import static software.wings.beans.WorkflowType.PIPELINE;

import com.google.inject.Inject;

import com.jayway.jsonpath.DocumentContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.query.CountOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ExpressionEvaluator;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WebHookService;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class WebHookServiceImpl implements WebHookService {
  @Inject private TriggerService triggerService;
  @Inject private AppService appService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;

  private static final Logger logger = LoggerFactory.getLogger(WebHookServiceImpl.class);

  private String getBaseUrl() {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }

  private String getUiUrl(
      boolean isPipeline, String accountId, String appId, String envId, String workflowExecutionId) {
    if (isPipeline) {
      return format("%s#/account/%s/app/%s/pipeline-execution/%s/workflow-execution/undefined/details", getBaseUrl(),
          accountId, appId, workflowExecutionId);
    } else {
      return format("%s#/account/%s/app/%s/env/%s/executions/%s/details", getBaseUrl(), accountId, appId, envId,
          workflowExecutionId);
    }
  }

  private String getApiUrl(String accountId, String appId, String workflowExecutionId) {
    return format("%sapi/external/v1/executions/%s/status?accountId=%s&appId=%s", getBaseUrl(), workflowExecutionId,
        accountId, appId);
  }

  @Override
  public WebHookResponse execute(String token, WebHookRequest webHookRequest) {
    try {
      logger.info("Received the webhookRequest {}  ", String.valueOf(webHookRequest));
      String appId = webHookRequest.getApplication();
      Application app = appService.get(appId);
      if (app == null) {
        return WebHookResponse.builder().error("Application does not exist").build();
      }

      Map<String, String> serviceBuildNumbers = new HashMap<>();
      List<Map<String, String>> artifacts = webHookRequest.getArtifacts();
      if (artifacts != null) {
        for (Map<String, String> artifact : artifacts) {
          String serviceName = artifact.get("service");
          String buildNumber = artifact.get("buildNumber");
          logger.info("Service name {} and Build Number {}", serviceName, buildNumber);
          if (serviceName != null) {
            if (wingsPersistence.createQuery(Service.class)
                    .filter("appId", appId)
                    .filter("name", serviceName)
                    .count(new CountOptions().limit(1))
                == 0) {
              return WebHookResponse.builder().error("Service Name [" + serviceName + "] does not exist").build();
            }
            serviceBuildNumbers.put(serviceName, buildNumber);
          }
        }
      }
      WorkflowExecution workflowExecution =
          triggerService.triggerExecutionByWebHook(appId, token, serviceBuildNumbers, webHookRequest.getParameters());
      return WebHookResponse.builder()
          .requestId(workflowExecution.getUuid())
          .status(workflowExecution.getStatus().name())
          .apiUrl(getApiUrl(app.getAccountId(), appId, workflowExecution.getUuid()))
          .uiUrl(getUiUrl(PIPELINE.equals(workflowExecution.getWorkflowType()), app.getAccountId(), appId,
              workflowExecution.getEnvId(), workflowExecution.getUuid()))
          .build();
    } catch (Exception ex) {
      return constructWebhookResponse(token, ex);
    }
  }

  private WebHookResponse constructWebhookResponse(String token, Exception ex) {
    logger.warn("WebHook call failed [%s]", token, ex);
    return WebHookResponse.builder().error(Misc.getMessage(ex).toLowerCase()).build();
  }

  @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
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
      return constructWebhookResponse(token, ex);
    }
  }
}

// generate requestId and save workflow executionId map
// queue multiple request
// compare queued requests
// wait for artifact to appear
// return response;
// Already running workflow
