package software.wings.service.impl;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookEventType.RELEASE;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.beans.trigger.WebhookSource.GITHUB;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.AccountLogContext;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.trigger.Action;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PayloadSource.Type;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.PrAction;
import software.wings.beans.trigger.ReleaseAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.TriggerExecution.Status;
import software.wings.beans.trigger.TriggerExecution.TriggerExecutionBuilder;
import software.wings.beans.trigger.TriggerExecution.WebhookEventDetails;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.trigger.WebhookEventUtils;
import software.wings.service.impl.trigger.WebhookTriggerDeploymentExecution;
import software.wings.service.impl.trigger.WebhookTriggerProcessor;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WebHookService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.service.intfc.trigger.TriggerExecutionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ValidateOnExecution
@Singleton
@Slf4j
public class WebHookServiceImpl implements WebHookService {
  public static final String X_GIT_HUB_EVENT = "X-GitHub-Event";
  public static final String X_GIT_LAB_EVENT = "X-Gitlab-Event";
  public static final String X_BIT_BUCKET_EVENT = "X-Event-Key";

  @Inject private TriggerService triggerService;
  @Inject private DeploymentTriggerService deploymentTriggerService;
  @Inject private AppService appService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private WebhookTriggerDeploymentExecution webhookTriggerDeploymentExecution;
  @Inject private WebhookEventUtils webhookEventUtils;
  @Inject private WebhookTriggerProcessor webhookTriggerProcessor;
  @Inject private TriggerExecutionService triggerExecutionService;
  @Inject private ServiceResourceService serviceResourceService;
  @Transient @Inject protected FeatureFlagService featureFlagService;

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
  public Response execute(String token, WebHookRequest webHookRequest) {
    try {
      if (webHookRequest == null) {
        logger.warn("Payload is mandatory");
        WebHookResponse webHookResponse = WebHookResponse.builder().error("Payload is mandatory").build();

        return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
      }

      logger.info("Received input Webhook Request {}  ", webHookRequest);
      Trigger trigger = triggerService.getTriggerByWebhookToken(token);
      DeploymentTrigger deploymentTrigger = deploymentTriggerService.getTriggerByWebhookToken(token);
      String accountId = getAccountId(trigger, deploymentTrigger);
      try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
        logger.info("Received the Webhook Request {} with accountId {}", webHookRequest, accountId);
      }
      if (featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
        if (deploymentTrigger == null) {
          WebHookResponse webHookResponse =
              WebHookResponse.builder().error("No trigger associated with the given token").build();

          return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
        }
        Application app = appService.get(deploymentTrigger.getAppId());
        if (app == null) {
          WebHookResponse webHookResponse =
              WebHookResponse.builder().error("No App associated with the given token").build();

          return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
        }
        return executeDepTriggerWebRequest(deploymentTrigger.getAppId(), token, app, webHookRequest);
      } else {
        if (trigger == null) {
          WebHookResponse webHookResponse =
              WebHookResponse.builder().error("No trigger associated with the given token").build();

          return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
        }
        if (trigger.isDisabled()) {
          return prepareRejectedResponse();
        }
        Application app = appService.get(trigger.getAppId());
        if (app == null) {
          WebHookResponse webHookResponse =
              WebHookResponse.builder().error("No App associated with the given token").build();

          return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
        }
        return executeTriggerWebRequest(trigger.getAppId(), token, app, webHookRequest);
      }

    } catch (WingsException ex) {
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      return prepareResponse(
          WebHookResponse.builder().error(ExceptionUtils.getMessage(ex)).build(), Response.Status.BAD_REQUEST);
    } catch (Exception ex) {
      logger.warn("Webhook Request call failed", ex);
      return prepareResponse(WebHookResponse.builder().error(ExceptionUtils.getMessage(ex)).build(),
          Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private Response executeDepTriggerWebRequest(
      String appId, String token, Application app, WebHookRequest webHookRequest) {
    DeploymentTrigger deploymentTrigger = deploymentTriggerService.getTriggerByWebhookToken(token);

    Map<String, Map<String, ArtifactSummary>> serviceBuildNumbers = new HashMap<>();
    Map<String, Object> payLoadMap = new HashMap<>();
    if (webHookRequest.getParameters() != null) {
      payLoadMap = webHookRequest.getParameters().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue()));
    }
    Response response =
        resolveServiceBuildNumbersWithArtifactVariables(appId, webHookRequest, serviceBuildNumbers, payLoadMap);

    if (response != null) {
      return response;
    }

    WorkflowExecution workflowExecution =
        webhookTriggerDeploymentExecution.validateExpressionsAndCustomTriggerDeployment(
            deploymentTrigger, payLoadMap, serviceBuildNumbers);

    return constructSuccessResponse(appId, app, workflowExecution);
  }

  private Response executeTriggerWebRequest(
      String appId, String token, Application app, WebHookRequest webHookRequest) {
    Map<String, ArtifactSummary> serviceBuildNumbers = new HashMap<>();

    Response response = resolveServiceBuildNumbers(appId, webHookRequest, serviceBuildNumbers);
    if (response != null) {
      return response;
    }
    WorkflowExecution workflowExecution = triggerService.triggerExecutionByWebHook(
        appId, token, serviceBuildNumbers, webHookRequest.getParameters(), TriggerExecution.builder().build());

    return constructSuccessResponse(appId, app, workflowExecution);
  }
  private boolean isGithubPingEvent(HttpHeaders httpHeaders) {
    if (httpHeaders == null) {
      return false;
    }
    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(httpHeaders);

    if (WebhookSource.GITHUB != webhookSource) {
      return false;
    }

    return WebhookEventType.PING.getValue().equals(webhookEventUtils.obtainEventType(webhookSource, httpHeaders));
  }

  @Override
  public Response executeByEvent(String token, String webhookEventPayload, HttpHeaders httpHeaders) {
    logger.info("Received the Webhook Request for token token {}  ", token);
    TriggerExecutionBuilder triggerExecutionBuilder = TriggerExecution.builder();
    TriggerExecution triggerExecution = triggerExecutionBuilder.build();
    try {
      Trigger trigger = triggerService.getTriggerByWebhookToken(token);
      DeploymentTrigger deploymentTrigger = deploymentTriggerService.getTriggerByWebhookToken(token);
      String accountId = getAccountId(trigger, deploymentTrigger);
      if (accountId == null) {
        WebHookResponse webHookResponse =
            WebHookResponse.builder().error("Trigger or account not associated to the given token").build();

        return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
      }

      if (featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
        return executeDeploymentTrigger(webhookEventPayload, httpHeaders, triggerExecution, deploymentTrigger);
      } else {
        return executeTrigger(webhookEventPayload, httpHeaders, triggerExecution, trigger);
      }

    } catch (WingsException ex) {
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      triggerExecution.setStatus(Status.FAILED);
      triggerExecution.setMessage(ExceptionUtils.getMessage(ex));
      triggerExecutionService.save(triggerExecution);
      WebHookResponse webHookResponse = WebHookResponse.builder().error(triggerExecution.getMessage()).build();

      return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
    } catch (Exception ex) {
      logger.error("Webhook Request call failed", ex);
      triggerExecution.setStatus(Status.FAILED);
      triggerExecution.setMessage(ExceptionUtils.getMessage(ex));
      triggerExecutionService.save(triggerExecution);
      WebHookResponse webHookResponse = WebHookResponse.builder().error(triggerExecution.getMessage()).build();

      return prepareResponse(webHookResponse, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private String getAccountId(Trigger trigger, DeploymentTrigger deploymentTrigger) {
    if (trigger == null) {
      if (deploymentTrigger != null) {
        return deploymentTrigger.getAccountId();
      } else {
        return null;
      }
    } else {
      return appService.getAccountIdByAppId(trigger.getAppId());
    }
  }

  private Response executeDeploymentTrigger(String webhookEventPayload, HttpHeaders httpHeaders,
      TriggerExecution triggerExecution, DeploymentTrigger deploymentTrigger) {
    if (deploymentTrigger == null) {
      WebHookResponse webHookResponse =
          WebHookResponse.builder().error("Trigger not associated to the given token").build();

      return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
    }

    Type payloadType = ((WebhookCondition) deploymentTrigger.getCondition()).getPayloadSource().getType();

    if (payloadType != Type.CUSTOM && isGithubPingEvent(httpHeaders)) {
      return prepareResponse(WebHookResponse.builder().message("Received ping event").build(), Response.Status.OK);
    }

    triggerExecution.setAppId(deploymentTrigger.getAppId());
    triggerExecution.setWebhookToken(
        ((WebhookCondition) (deploymentTrigger.getCondition())).getWebHookToken().getWebHookToken());
    triggerExecution.setTriggerId(deploymentTrigger.getUuid());
    triggerExecution.setTriggerName(deploymentTrigger.getName());
    triggerExecution.setWebhookEventDetails(WebhookEventDetails.builder().build());
    triggerExecution.setWorkflowId(getWorkflowId(deploymentTrigger.getAction()));
    triggerExecution.setWorkflowType(getWorkflowType(deploymentTrigger.getAction().getActionType()));

    WorkflowExecution workflowExecution = null;
    try {
      if (payloadType == Type.CUSTOM) {
        logger.info("Executing custom request for trigger {}  ", deploymentTrigger.getName());
        Map<String, Object> payLoadMap =
            JsonUtils.asObject(webhookEventPayload, new TypeReference<Map<String, Object>>() {});
        workflowExecution = webhookTriggerDeploymentExecution.validateExpressionsAndCustomTriggerDeployment(
            deploymentTrigger, payLoadMap, null);
      } else {
        logger.info("Executing git request for trigger {}  ", deploymentTrigger.getName());
        workflowExecution = webhookTriggerDeploymentExecution.validateExpressionsAndTriggerDeployment(
            webhookEventPayload, httpHeaders, deploymentTrigger, triggerExecution.getWebhookEventDetails());
      }
    } catch (WingsException ex) {
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      triggerExecution.setMessage(ExceptionUtils.getMessage(ex));
      triggerExecution.setStatus(Status.REJECTED);
      triggerExecutionService.save(triggerExecution);
      WebHookResponse webHookResponse = WebHookResponse.builder().error(triggerExecution.getMessage()).build();

      return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
    }

    if (webhookTriggerProcessor.checkFileContentOptionSelected(deploymentTrigger)) {
      WebHookResponse webHookResponse =
          WebHookResponse.builder()
              .message("Request received. Deployment will be triggered if the file content changed")
              .build();

      return prepareResponse(webHookResponse, Response.Status.OK);
    } else {
      logger.info("Execution trigger success. Saving trigger execution");
      WebHookResponse webHookResponse = WebHookResponse.builder()
                                            .requestId(workflowExecution.getUuid())
                                            .status(workflowExecution.getStatus().name())
                                            .build();

      return prepareResponse(webHookResponse, Response.Status.OK);
    }
  }

  private WorkflowType getWorkflowType(ActionType actionType) {
    if (actionType == ActionType.WORKFLOW) {
      return ORCHESTRATION;
    } else if (actionType == ActionType.PIPELINE) {
      return PIPELINE;
    } else {
      throw new InvalidRequestException("Invalid action type " + actionType.name());
    }
  }

  private String getWorkflowId(Action action) {
    if (action.getActionType() == ActionType.WORKFLOW) {
      return ((WorkflowAction) action).getWorkflowId();
    } else if (action.getActionType() == ActionType.PIPELINE) {
      return ((PipelineAction) action).getPipelineId();
    } else {
      throw new InvalidRequestException("Invalid action type " + action.getActionType().name());
    }
  }

  private Response executeTrigger(
      String webhookEventPayload, HttpHeaders httpHeaders, TriggerExecution triggerExecution, Trigger trigger) {
    if (trigger == null) {
      WebHookResponse webHookResponse =
          WebHookResponse.builder().error("Trigger not associated to the given token").build();

      return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
    }

    if (isGithubPingEvent(httpHeaders)) {
      return prepareResponse(WebHookResponse.builder().message("Received ping event").build(), Response.Status.OK);
    }

    if (trigger.isDisabled()) {
      return prepareRejectedResponse();
    }

    triggerExecution.setAppId(trigger.getAppId());
    triggerExecution.setWebhookToken(trigger.getWebHookToken());
    triggerExecution.setTriggerId(trigger.getUuid());
    triggerExecution.setTriggerName(trigger.getName());
    triggerExecution.setWebhookEventDetails(WebhookEventDetails.builder().build());
    triggerExecution.setWorkflowId(trigger.getWorkflowId());
    triggerExecution.setWorkflowType(trigger.getWorkflowType());

    Map<String, String> resolvedParameters;
    try {
      resolvedParameters = resolveWebhookParameters(
          webhookEventPayload, httpHeaders, trigger, triggerExecution.getWebhookEventDetails());
      // Validate the give branch name matches the one with selected one
      webhookTriggerProcessor.validateBranchName(trigger, triggerExecution);
    } catch (WingsException ex) {
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      triggerExecution.setMessage(ExceptionUtils.getMessage(ex));
      triggerExecution.setStatus(Status.REJECTED);
      triggerExecutionService.save(triggerExecution);
      WebHookResponse webHookResponse = WebHookResponse.builder().error(triggerExecution.getMessage()).build();

      return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
    }

    logger.info("Trigger execution for the trigger {}", trigger.getUuid());
    WorkflowExecution workflowExecution =
        triggerService.triggerExecutionByWebHook(trigger, resolvedParameters, triggerExecution);
    if (webhookTriggerProcessor.checkFileContentOptionSelected(trigger)) {
      WebHookResponse webHookResponse =
          WebHookResponse.builder()
              .message("Request received. Deployment will be triggered if the file content changed")
              .build();

      return prepareResponse(webHookResponse, Response.Status.OK);
    } else {
      logger.info("Execution trigger success. Saving trigger execution");
      WebHookResponse webHookResponse = WebHookResponse.builder()
                                            .requestId(workflowExecution.getUuid())
                                            .status(workflowExecution.getStatus().name())
                                            .build();

      return prepareResponse(webHookResponse, Response.Status.OK);
    }
  }

  private Response prepareRejectedResponse() {
    WebHookResponse webHookResponse =
        WebHookResponse.builder().error("Trigger rejected due to slowness in the product. Please try again").build();

    return prepareResponse(webHookResponse, Response.Status.SERVICE_UNAVAILABLE);
  }

  private Response resolveServiceBuildNumbers(
      String appId, WebHookRequest webHookRequest, Map<String, ArtifactSummary> serviceArtifactMapping) {
    List<Map<String, String>> artifacts = webHookRequest.getArtifacts();
    if (artifacts != null) {
      for (Map<String, String> artifact : artifacts) {
        String serviceName = artifact.get("service");
        String buildNumber = artifact.get("buildNumber");
        String artifactStreamName = artifact.get("artifactSourceName");
        logger.info("WebHook params Service name {}, Build Number {} and Artifact Source Name {}", serviceName,
            buildNumber, artifactStreamName);
        if (serviceName != null) {
          Service service = serviceResourceService.getServiceByName(appId, serviceName, false);
          if (service == null) {
            return prepareResponse(
                WebHookResponse.builder().error("Service Name [" + serviceName + "] does not exist").build(),
                Response.Status.BAD_REQUEST);
          }
          serviceArtifactMapping.put(
              service.getUuid(), ArtifactSummary.builder().name(artifactStreamName).buildNo(buildNumber).build());
        }
      }
    }
    return null;
  }

  private Response resolveServiceBuildNumbersWithArtifactVariables(String appId, WebHookRequest webHookRequest,
      Map<String, Map<String, ArtifactSummary>> serviceArtifactMapping, Map<String, Object> payLoadMap) {
    List<Map<String, String>> artifacts = webHookRequest.getArtifacts();
    if (artifacts != null) {
      for (Map<String, String> artifact : artifacts) {
        String buildNumber = artifact.get("buildNumber");
        String artifactStreamName = artifact.get("artifactSourceName");
        String serviceName = artifact.get("service");
        String artifactVariableName = ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME;

        String appName = appService.get(appId).getName();
        if (artifactStreamName != null) {
          payLoadMap.put(appName + "_" + serviceName + "_" + artifactStreamName, (Object) buildNumber);
        }

        logger.info(
            "WebHook params Service name {}, Artifact Variable Name {}, Build Number {} and Artifact Source Name {}",
            serviceName, artifactVariableName, buildNumber, artifactStreamName);
        if (serviceName != null) {
          Service service = serviceResourceService.getServiceByName(appId, serviceName, false);
          if (service == null) {
            return prepareResponse(
                WebHookResponse.builder().error("Service Name [" + serviceName + "] does not exist").build(),
                Response.Status.BAD_REQUEST);
          }

          if (serviceArtifactMapping.containsKey(service.getUuid())) {
            Map<String, ArtifactSummary> stringArtifactSummaryMap = serviceArtifactMapping.get(service.getUuid());
            stringArtifactSummaryMap.put(artifactVariableName,
                ArtifactSummary.builder().artifactSourceName(artifactStreamName).buildNo(buildNumber).build());

            serviceArtifactMapping.put(service.getUuid(), stringArtifactSummaryMap);
          } else {
            Map<String, ArtifactSummary> serviceBuildNumbers = new HashMap<>();
            serviceBuildNumbers.put(artifactVariableName,
                ArtifactSummary.builder().artifactSourceName(artifactStreamName).buildNo(buildNumber).build());
            serviceArtifactMapping.put(service.getUuid(), serviceBuildNumbers);
          }
        }
      }
    }
    return null;
  }

  private Map<String, String> resolveWebhookParameters(
      String payload, HttpHeaders httpHeaders, Trigger trigger, WebhookEventDetails webhookEventDetails) {
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
    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(httpHeaders);

    if (webhookSource != webhookTriggerCondition.getWebhookSource()) {
      String msg = "Trigger [" + trigger.getName() + "] is set for source ["
          + webhookTriggerCondition.getWebhookSource() + "] not associate with the in coming source   [" + webhookSource
          + "]";
      throw new InvalidRequestException(msg, USER);
    }

    Map<String, Object> payLoadMap = JsonUtils.asObject(payload, new TypeReference<Map<String, Object>>() {});

    String branchName = webhookEventUtils.obtainBranchName(webhookSource, httpHeaders, payLoadMap);
    String storedBranchRegex = webhookTriggerCondition.getBranchRegex();
    if (EmptyPredicate.isNotEmpty(storedBranchRegex) && EmptyPredicate.isNotEmpty(branchName)) {
      validateBranchWithRegex(storedBranchRegex, branchName);
    }
    validateWebHook(webhookSource, trigger, webhookTriggerCondition, payLoadMap, httpHeaders);
    webhookEventDetails.setPayload(payload);
    webhookEventDetails.setBranchName(branchName);
    webhookEventDetails.setCommitId(webhookEventUtils.obtainCommitId(webhookSource, httpHeaders, payLoadMap));
    webhookEventDetails.setWebhookSource(webhookSource.name());
    webhookEventDetails.setWebhookEventType(webhookEventUtils.obtainEventType(webhookSource, httpHeaders));
    webhookEventDetails.setPrAction(webhookEventUtils.obtainPrAction(webhookSource, payLoadMap));
    webhookEventDetails.setGitConnectorId(webhookTriggerCondition.getGitConnectorId());
    webhookEventDetails.setFilePaths(webhookTriggerCondition.getFilePaths());

    for (Entry<String, String> entry : workflowVariables.entrySet()) {
      String param = entry.getKey();
      String paramValue = entry.getValue();
      try {
        if (isNotEmpty(paramValue)) {
          Object evalutedValue = expressionEvaluator.substitute(paramValue, payLoadMap);
          if (evalutedValue != null) {
            resolvedParameters.put(param, String.valueOf(evalutedValue));
          } else {
            resolvedParameters.put(param, paramValue);
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to resolve the param {} in Json {}", param, payload);
      }
    }
    webhookEventDetails.setParameters(resolvedParameters);
    return resolvedParameters;
  }

  @VisibleForTesting
  void validateWebHook(WebhookSource webhookSource, Trigger trigger, WebHookTriggerCondition triggerCondition,
      Map<String, Object> payLoadMap, HttpHeaders httpHeaders) {
    if (WebhookSource.GITHUB == webhookSource) {
      validateGitHubWebhook(trigger, triggerCondition, payLoadMap, httpHeaders);
    } else if (WebhookSource.BITBUCKET == webhookSource) {
      validateBitBucketWebhook(trigger, triggerCondition, httpHeaders);
    }
  }

  private void validateBranchWithRegex(String storedBranch, String inputBranchName) {
    if (Pattern.compile(storedBranch).matcher(inputBranchName).matches()) {
      return;
    }
    String msg = String.format(
        "WebHook event branch name filter [%s] does not match with the trigger condition branch name [%s]",
        inputBranchName, storedBranch);
    throw new InvalidRequestException(msg, WingsException.USER);
  }

  private void validateBitBucketWebhook(
      Trigger trigger, WebHookTriggerCondition triggerCondition, HttpHeaders headers) {
    WebhookSource webhookSource = triggerCondition.getWebhookSource();
    if (BITBUCKET == webhookSource) {
      logger.info("Trigger is set for BitBucket. Checking the http headers for the request type");
      String bitBucketEvent = headers == null ? null : headers.getHeaderString(X_BIT_BUCKET_EVENT);
      logger.info("X-Event-Key is {} ", bitBucketEvent);
      if (bitBucketEvent == null) {
        throw new InvalidRequestException("Header [X-Event-Key] is missing", USER);
      }

      BitBucketEventType bitBucketEventType = BitBucketEventType.find(bitBucketEvent);
      String errorMsg = "Trigger [" + trigger.getName() + "] is not associated with the received BitBucket event ["
          + bitBucketEvent + "]";

      if (triggerCondition.getBitBucketEvents() != null
          && ((triggerCondition.getBitBucketEvents().contains(bitBucketEventType)
                 || (BitBucketEventType.containsAllEvent(triggerCondition.getBitBucketEvents()))))) {
        return;
      } else {
        throw new InvalidRequestException(errorMsg, USER);
      }
    }
  }

  private void validateGitHubWebhook(
      Trigger trigger, WebHookTriggerCondition triggerCondition, Map<String, Object> content, HttpHeaders headers) {
    WebhookSource webhookSource = triggerCondition.getWebhookSource();
    if (GITHUB == webhookSource) {
      logger.info("Trigger is set for GitHub. Checking the http headers for the request type");
      String gitHubEvent = headers == null ? null : headers.getHeaderString(X_GIT_HUB_EVENT);
      logger.info("X-GitHub-Event is {} ", gitHubEvent);
      if (gitHubEvent == null) {
        throw new InvalidRequestException("Header [X-GitHub-Event] is missing", USER);
      }
      WebhookEventType webhookEventType = WebhookEventType.find(gitHubEvent);

      String errorMsg =
          "Trigger [" + trigger.getName() + "] is not associated with the received GitHub event [" + gitHubEvent + "]";

      validateEventType(triggerCondition, content, errorMsg, webhookEventType);
      if (PULL_REQUEST == webhookEventType || RELEASE == webhookEventType) {
        Object gitEventAction = content.get("action");

        String msg = "Trigger [" + trigger.getName() + "] is not associated with the received GitHub action ["
            + gitEventAction + "]";

        if (gitEventAction != null) {
          if (triggerCondition.getActions() != null && webhookEventType == PULL_REQUEST
              && !triggerCondition.getActions().contains(PrAction.find(gitEventAction.toString()))) {
            throw new InvalidRequestException(msg, USER);
          }
          if (triggerCondition.getReleaseActions() != null && webhookEventType == RELEASE
              && !triggerCondition.getReleaseActions().contains(ReleaseAction.find(gitEventAction.toString()))) {
            throw new InvalidRequestException(msg, USER);
          }
        }
      }
    }
  }

  private void validateEventType(WebHookTriggerCondition triggerCondition, Map<String, Object> content,
      String errorMessage, WebhookEventType webhookEventType) {
    if (triggerCondition.getEventTypes() != null && !triggerCondition.getEventTypes().contains(webhookEventType)) {
      throw new InvalidRequestException(errorMessage, USER);
    }
  }

  private Response constructSuccessResponse(String appId, Application app, WorkflowExecution workflowExecution) {
    WebHookResponse webHookResponse =
        WebHookResponse.builder()
            .requestId(workflowExecution.getUuid())
            .status(workflowExecution.getStatus().name())
            .apiUrl(getApiUrl(app.getAccountId(), appId, workflowExecution.getUuid()))
            .uiUrl(getUiUrl(PIPELINE == workflowExecution.getWorkflowType(), app.getAccountId(), appId,
                workflowExecution.getEnvId(), workflowExecution.getUuid()))
            .build();

    return prepareResponse(webHookResponse, Response.Status.OK);
  }

  private Response prepareResponse(WebHookResponse webhookResponse, Response.Status status) {
    return Response.status(status).entity(webhookResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
