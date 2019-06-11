package software.wings.service.impl;

import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.beans.trigger.WebhookSource.GITHUB;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.trigger.PrAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.TriggerExecution.Status;
import software.wings.beans.trigger.TriggerExecution.TriggerExecutionBuilder;
import software.wings.beans.trigger.TriggerExecution.WebhookEventDetails;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.trigger.WebhookEventUtils;
import software.wings.service.impl.trigger.WebhookTriggerProcessor;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WebHookService;
import software.wings.service.intfc.trigger.TriggerExecutionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
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
  @Inject private AppService appService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
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
      logger.info("Received the Webhook Request {}  ", String.valueOf(webHookRequest));
      String appId = webHookRequest.getApplication();
      Application app = appService.get(appId);
      if (app == null) {
        WebHookResponse webHookResponse = WebHookResponse.builder().error("Application does not exist").build();
        return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
      }

      Map<String, ArtifactSummary> serviceBuildNumbers = new HashMap<>();

      Response response = resolveServiceBuildNumbers(appId, webHookRequest, serviceBuildNumbers);
      if (response != null) {
        return response;
      }
      WorkflowExecution workflowExecution = triggerService.triggerExecutionByWebHook(
          appId, token, serviceBuildNumbers, webHookRequest.getParameters(), TriggerExecution.builder().build());

      return constructSuccessResponse(appId, app, workflowExecution);
    } catch (WingsException ex) {
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      return prepareResponse(
          WebHookResponse.builder().error(ExceptionUtils.getMessage(ex)).build(), Response.Status.BAD_REQUEST);
    } catch (Exception ex) {
      logger.warn(format("Webhook Request call failed"), ex);
      return prepareResponse(WebHookResponse.builder().error(ExceptionUtils.getMessage(ex)).build(),
          Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean isGithubPingEvent(HttpHeaders httpHeaders) {
    WebhookSource webhookSource = webhookEventUtils.obtainWebhookSource(httpHeaders);

    if (!WebhookSource.GITHUB.equals(webhookSource)) {
      return false;
    }

    return WebhookEventType.PING.getValue().equals(webhookEventUtils.obtainEventType(webhookSource, httpHeaders));
  }

  @Override
  public Response executeByEvent(String token, String webhookEventPayload, HttpHeaders httpHeaders) {
    TriggerExecutionBuilder triggerExecutionBuilder = TriggerExecution.builder();
    TriggerExecution triggerExecution = triggerExecutionBuilder.build();
    try {
      Trigger trigger = triggerService.getTriggerByWebhookToken(token);
      if (trigger == null) {
        WebHookResponse webHookResponse =
            WebHookResponse.builder().error("Trigger not associated to the given token").build();

        return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
      }

      if (isGithubPingEvent(httpHeaders)) {
        return prepareResponse(WebHookResponse.builder().message("Received ping event").build(), Response.Status.OK);
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
    } catch (WingsException ex) {
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      triggerExecution.setStatus(Status.FAILED);
      triggerExecution.setMessage(ExceptionUtils.getMessage(ex));
      triggerExecutionService.save(triggerExecution);
      WebHookResponse webHookResponse = WebHookResponse.builder().error(triggerExecution.getMessage()).build();

      return prepareResponse(webHookResponse, Response.Status.BAD_REQUEST);
    } catch (Exception ex) {
      logger.error(format("Webhook Request call failed "), ex);
      triggerExecution.setStatus(Status.FAILED);
      triggerExecution.setMessage(ExceptionUtils.getMessage(ex));
      triggerExecutionService.save(triggerExecution);
      WebHookResponse webHookResponse = WebHookResponse.builder().error(triggerExecution.getMessage()).build();

      return prepareResponse(webHookResponse, Response.Status.INTERNAL_SERVER_ERROR);
    }
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

    if (!webhookSource.equals(webhookTriggerCondition.getWebhookSource())) {
      String msg = "Trigger [" + trigger.getName() + "] is set for source ["
          + webhookTriggerCondition.getWebhookSource() + "] not associate with the in coming source   [" + webhookSource
          + "]";
      throw new WingsException(msg, USER);
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

  private void validateWebHook(WebhookSource webhookSource, Trigger trigger, WebHookTriggerCondition triggerCondition,
      Map<String, Object> payLoadMap, HttpHeaders httpHeaders) {
    if (WebhookSource.GITHUB.equals(webhookSource)) {
      validateGitHubWebhook(trigger, triggerCondition, payLoadMap, httpHeaders);
    } else if (WebhookSource.BITBUCKET.equals(webhookSource)) {
      validateBitBucketWebhook(trigger, triggerCondition, payLoadMap, httpHeaders);
    }
  }

  private void validateBranchWithRegex(String storedBranch, String inputBranchName) {
    if (Pattern.compile(storedBranch).matcher(inputBranchName).matches()) {
      return;
    }
    String msg = String.format(
        "WebHook event branch name filter [%s] does not match with the trigger condition branch name [%s]",
        inputBranchName, storedBranch);
    throw new WingsException(msg, WingsException.USER);
  }

  private void validateBitBucketWebhook(
      Trigger trigger, WebHookTriggerCondition triggerCondition, Map<String, Object> content, HttpHeaders headers) {
    WebhookSource webhookSource = triggerCondition.getWebhookSource();
    if (BITBUCKET.equals(webhookSource)) {
      logger.info("Trigger is set for BitBucket. Checking the http headers for the request type");
      String bitBucketEvent = headers == null ? null : headers.getHeaderString(X_BIT_BUCKET_EVENT);
      logger.info("X-Event-Key is {} ", bitBucketEvent);
      if (bitBucketEvent == null) {
        throw new WingsException("Header [X-Event-Key] is missing", USER);
      }

      BitBucketEventType bitBucketEventType = BitBucketEventType.find(bitBucketEvent);
      String errorMsg = "Trigger [" + trigger.getName() + "] is not associated with the received BitBucket event ["
          + bitBucketEvent + "]";

      if (triggerCondition.getBitBucketEvents() != null
          && ((triggerCondition.getBitBucketEvents().contains(bitBucketEventType)
                 || (BitBucketEventType.containsAllEvent(triggerCondition.getBitBucketEvents()))))) {
        return;
      } else {
        throw new WingsException(errorMsg, USER);
      }
    }
  }

  private void validateGitHubWebhook(
      Trigger trigger, WebHookTriggerCondition triggerCondition, Map<String, Object> content, HttpHeaders headers) {
    WebhookSource webhookSource = triggerCondition.getWebhookSource();
    if (GITHUB.equals(webhookSource)) {
      logger.info("Trigger is set for GitHub. Checking the http headers for the request type");
      String gitHubEvent = headers == null ? null : headers.getHeaderString(X_GIT_HUB_EVENT);
      logger.info("X-GitHub-Event is {} ", gitHubEvent);
      if (gitHubEvent == null) {
        throw new WingsException("Header [X-GitHub-Event] is missing", USER);
      }
      WebhookEventType webhookEventType = WebhookEventType.find(gitHubEvent);

      String errorMsg =
          "Trigger [" + trigger.getName() + "] is not associated with the received GitHub event [" + gitHubEvent + "]";

      validateEventType(triggerCondition, content, errorMsg, webhookEventType);
      if (PULL_REQUEST.equals(webhookEventType)) {
        Object prAction = content.get("action");
        if (prAction != null && triggerCondition.getActions() != null
            && !triggerCondition.getActions().contains(PrAction.find(prAction.toString()))) {
          String msg = "Trigger [" + trigger.getName() + "] is not associated with the received GitHub action ["
              + prAction + "]";
          throw new WingsException(msg, USER);
        }
      }
    }
  }

  private void validateEventType(WebHookTriggerCondition triggerCondition, Map<String, Object> content,
      String errorMessage, WebhookEventType webhookEventType) {
    if (triggerCondition.getEventTypes() != null && !triggerCondition.getEventTypes().contains(webhookEventType)) {
      throw new WingsException(errorMessage, USER);
    }
  }

  private Response constructSuccessResponse(String appId, Application app, WorkflowExecution workflowExecution) {
    WebHookResponse webHookResponse =
        WebHookResponse.builder()
            .requestId(workflowExecution.getUuid())
            .status(workflowExecution.getStatus().name())
            .apiUrl(getApiUrl(app.getAccountId(), appId, workflowExecution.getUuid()))
            .uiUrl(getUiUrl(PIPELINE.equals(workflowExecution.getWorkflowType()), app.getAccountId(), appId,
                workflowExecution.getEnvId(), workflowExecution.getUuid()))
            .build();

    return prepareResponse(webHookResponse, Response.Status.OK);
  }

  private Response prepareResponse(WebHookResponse webhookResponse, Response.Status status) {
    return Response.status(status).entity(webhookResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
