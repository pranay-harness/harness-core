package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookSource.GITHUB;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.exception.WingsException;
import org.mongodb.morphia.query.CountOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.PrAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
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
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.HttpHeaders;

@ValidateOnExecution
public class WebHookServiceImpl implements WebHookService {
  public static final String X_GIT_HUB_EVENT = "X-GitHub-Event";
  @Inject private TriggerService triggerService;
  @Inject private AppService appService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExpressionEvaluator expressionEvaluator;

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
      WebHookResponse webHookResponse = resolveServiceBuildNumbers(appId, webHookRequest, serviceBuildNumbers);
      if (webHookResponse != null) {
        return webHookResponse;
      }
      WorkflowExecution workflowExecution =
          triggerService.triggerExecutionByWebHook(appId, token, serviceBuildNumbers, webHookRequest.getParameters());

      return constructWebhookResponse(appId, app, workflowExecution);
    } catch (Exception ex) {
      return constructWebhookResponse(token, ex);
    }
  }

  @Override
  public WebHookResponse executeByEvent(String token, String webhookEventPayload, HttpHeaders httpHeaders) {
    try {
      logger.debug("Received the webhook event payload {}", webhookEventPayload);
      Trigger trigger = triggerService.getTriggerByWebhookToken(token);
      if (trigger == null) {
        return WebHookResponse.builder().error("Trigger not associated to the given token").build();
      }
      Map<String, String> resolvedParameters = resolveWebhookParameters(webhookEventPayload, httpHeaders, trigger);
      logger.info("Triggering  execution");
      WorkflowExecution workflowExecution = triggerService.triggerExecutionByWebHook(trigger, resolvedParameters);
      logger.info("Execution trigger success");
      return WebHookResponse.builder()
          .requestId(workflowExecution.getUuid())
          .status(workflowExecution.getStatus().name())
          .build();

    } catch (Exception ex) {
      return constructWebhookResponse(token, ex);
    }
  }

  private WebHookResponse resolveServiceBuildNumbers(
      String appId, WebHookRequest webHookRequest, Map<String, String> serviceBuildNumbers) {
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
    return null;
  }

  private Map<String, String> resolveWebhookParameters(
      String webhookEventPayload, HttpHeaders httpHeaders, Trigger trigger) {
    WebHookTriggerCondition webhookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();

    // Web hook parameters saved
    Map<String, String> webhookParameters =
        webhookTriggerCondition.getParameters() == null ? new HashMap<>() : webhookTriggerCondition.getParameters();

    // Add the workflow variables to evaluate from Payload
    Map<String, String> workflowVariables =
        trigger.getWorkflowVariables() == null ? new HashMap<>() : trigger.getWorkflowVariables();
    for (Map.Entry<String, String> parameterEntry : workflowVariables.entrySet()) {
      if (webhookParameters.containsKey(parameterEntry.getKey())) {
        // Override it from parameters
        workflowVariables.put(parameterEntry.getKey(), webhookParameters.get(parameterEntry.getKey()));
      }
    }

    Map<String, String> resolvedParameters = new HashMap<>();

    Map<String, Object> map = JsonUtils.asObject(webhookEventPayload, new TypeReference<Map<String, Object>>() {});
    validateGitHubWebhook(trigger, webhookTriggerCondition, map, httpHeaders);

    for (Map.Entry<String, String> parameterEntry : workflowVariables.entrySet()) {
      String paramValue = parameterEntry.getValue();
      String param = parameterEntry.getKey();
      try {
        if (isNotEmpty(parameterEntry.getValue())) {
          Object evalutedValue = expressionEvaluator.substitute(paramValue, map);
          if (evalutedValue != null) {
            resolvedParameters.put(param, String.valueOf(evalutedValue));
          } else {
            resolvedParameters.put(param, paramValue);
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to resolve the param {} in Json {}", param, webhookEventPayload);
      }
    }

    return resolvedParameters;
  }

  private void validateGitHubWebhook(
      Trigger trigger, WebHookTriggerCondition triggerCondition, Map<String, Object> content, HttpHeaders headers) {
    WebhookSource webhookSource = triggerCondition.getWebhookSource();
    if (GITHUB.equals(webhookSource)) {
      logger.info("Trigger is set for GitHub. Checking the http headers for the request type");
      String gitHubEvent = headers == null ? null : headers.getHeaderString(X_GIT_HUB_EVENT);
      logger.info("X-GitHub-Event is {} ", gitHubEvent);
      if (gitHubEvent == null) {
        throw new WingsException("Header [X-GitHub-Event] is missing");
      }
      WebhookEventType webhookEventType = WebhookEventType.find(gitHubEvent);
      if (triggerCondition.getEventTypes() != null && !triggerCondition.getEventTypes().contains(webhookEventType)) {
        String msg = "Trigger [" + trigger.getName() + "] is not associated with the received GitHub event ["
            + gitHubEvent + "]";
        logger.warn(msg);
        throw new WingsException(msg);
      }
      if (PULL_REQUEST.equals(webhookEventType)) {
        Object prAction = content.get("action");
        if (prAction != null && triggerCondition.getActions() != null
            && !triggerCondition.getActions().contains(PrAction.find(prAction.toString()))) {
          String msg = "Trigger [" + trigger.getName() + "] is not associated with the received GitHub action ["
              + prAction + "]";
          logger.warn(msg);
          throw new WingsException(msg);
        }
      }
    }
  }

  private WebHookResponse constructWebhookResponse(String appId, Application app, WorkflowExecution workflowExecution) {
    return WebHookResponse.builder()
        .requestId(workflowExecution.getUuid())
        .status(workflowExecution.getStatus().name())
        .apiUrl(getApiUrl(app.getAccountId(), appId, workflowExecution.getUuid()))
        .uiUrl(getUiUrl(PIPELINE.equals(workflowExecution.getWorkflowType()), app.getAccountId(), appId,
            workflowExecution.getEnvId(), workflowExecution.getUuid()))
        .build();
  }

  private WebHookResponse constructWebhookResponse(String token, Exception ex) {
    logger.warn(format("Webhook Request call failed [%s]", token), ex);
    return WebHookResponse.builder().error(Misc.getMessage(ex)).build();
  }
}

// generate requestId and save workflow executionId map
// queue multiple request
// compare queued requests
// wait for artifact to appear
// return response;
// Already running workflow
