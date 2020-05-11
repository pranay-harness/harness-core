package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.trigger.WebhookEventType.ANY;
import static software.wings.beans.trigger.WebhookEventType.ISSUE;
import static software.wings.beans.trigger.WebhookEventType.OTHER;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookEventType.PUSH;
import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.ISSUE_ANY;
import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.PULL_REQUEST_ANY;
import static software.wings.beans.trigger.WebhookSource.BitBucketEventType.PUSH_ANY;
import static software.wings.service.impl.trigger.WebhookEventUtils.X_BIT_BUCKET_EVENT;
import static software.wings.service.impl.trigger.WebhookEventUtils.X_GIT_HUB_EVENT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.TriggerException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.trigger.BitBucketPayloadSource;
import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.GitHubPayloadSource;
import software.wings.beans.trigger.GitLabsPayloadSource;
import software.wings.beans.trigger.PayloadSource.Type;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWebhook;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.TriggerExecution.WebhookEventDetails;
import software.wings.beans.trigger.TriggerLastDeployedType;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.HttpHeaders;

@OwnedBy(CDC)
@ValidateOnExecution
@Singleton
@Slf4j
public class WebhookTriggerDeploymentExecution {
  @Inject private WebhookEventUtils webhookEventUtils;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private DeploymentTriggerService deploymentTriggerService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;

  @Inject WorkflowService workflowService;
  @Inject PipelineService pipelineService;

  public WorkflowExecution validateExpressionsAndTriggerDeployment(String payload, HttpHeaders httpHeaders,
      DeploymentTrigger deploymentTrigger, WebhookEventDetails webhookEventDetails) {
    WebhookCondition webhookCondition = (WebhookCondition) deploymentTrigger.getCondition();

    Type webhookSource = webhookEventUtils.obtainEventType(httpHeaders);

    if (webhookSource != webhookCondition.getPayloadSource().getType()) {
      String msg = "Deployment Trigger [" + deploymentTrigger.getName() + "] is set for source ["
          + webhookCondition.getPayloadSource().getType() + "] not associate with the in coming source   ["
          + webhookSource + "]";
      throw new TriggerException(msg, USER);
    }

    Map<String, Object> payLoadMap = JsonUtils.asObject(payload, new TypeReference<Map<String, Object>>() {});

    validateStoredEventTypeFromInput(deploymentTrigger, httpHeaders, payLoadMap, webhookCondition);
    validateCustomPayloadExpressionFromInput(payLoadMap, webhookCondition, deploymentTrigger);

    Map<String, String> wfVariables = resolveWorkflowVariables(payLoadMap, deploymentTrigger);

    List<TriggerArtifactVariable> artifactVariables =
        resolveArtifactVariables(deploymentTrigger.getAppId(), payLoadMap, deploymentTrigger, null);

    logger.info("Trigger execution for the trigger {}", deploymentTrigger.getUuid());

    return deploymentTriggerService.triggerExecutionByWebHook(deploymentTrigger, wfVariables, artifactVariables, null);
  }

  public WorkflowExecution validateExpressionsAndCustomTriggerDeployment(DeploymentTrigger deploymentTrigger,
      Map<String, Object> payLoadMap, Map<String, Map<String, ArtifactSummary>> serviceArtifactMapping) {
    WebhookCondition webhookCondition = (WebhookCondition) deploymentTrigger.getCondition();

    validateCustomPayloadExpressionFromInput(payLoadMap, webhookCondition, deploymentTrigger);

    logger.info("resolving workflow variables for trigger {}  ", deploymentTrigger.getName());
    Map<String, String> wfVariables = resolveWorkflowVariables(payLoadMap, deploymentTrigger);

    logger.info("workflow variables reolved successfully for trigger {}  ", deploymentTrigger.getName());
    List<TriggerArtifactVariable> artifactVariables =
        resolveArtifactVariables(deploymentTrigger.getAppId(), payLoadMap, deploymentTrigger, serviceArtifactMapping);

    logger.info("Trigger execution for the trigger {}", deploymentTrigger.getUuid());

    return deploymentTriggerService.triggerExecutionByWebHook(deploymentTrigger, wfVariables, artifactVariables, null);
  }

  private List<TriggerArtifactVariable> resolveArtifactVariables(String appId, Map<String, Object> payLoadMap,
      DeploymentTrigger deploymentTrigger, Map<String, Map<String, ArtifactSummary>> serviceArtifactMapping) {
    if (deploymentTrigger.getAction() != null) {
      switch (deploymentTrigger.getAction().getActionType()) {
        case PIPELINE:
          PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();

          if (pipelineAction.getTriggerArgs() != null
              && pipelineAction.getTriggerArgs().getTriggerArtifactVariables() != null) {
            TriggerArgs triggerArgs = pipelineAction.getTriggerArgs();
            List<TriggerArtifactVariable> triggerArtifactVariables =
                pipelineAction.getTriggerArgs()
                    .getTriggerArtifactVariables()
                    .stream()
                    .map(artifactVariable -> {
                      return fetchArtifactVariable(appId, artifactVariable, payLoadMap, serviceArtifactMapping);
                    })
                    .collect(toList());

            deploymentTrigger.setAction(
                PipelineAction.builder()
                    .pipelineName(pipelineAction.getPipelineName())
                    .pipelineId(pipelineAction.getPipelineId())
                    .triggerArgs(TriggerArgs.builder()
                                     .triggerArtifactVariables(triggerArtifactVariables)
                                     .excludeHostsWithSameArtifact(triggerArgs.isExcludeHostsWithSameArtifact())
                                     .variables(triggerArgs.getVariables())
                                     .build())
                    .build());

            return triggerArtifactVariables;
          }
          break;
        case WORKFLOW:
          WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
          if (workflowAction.getTriggerArgs() != null
              && workflowAction.getTriggerArgs().getTriggerArtifactVariables() != null) {
            TriggerArgs triggerArgs = workflowAction.getTriggerArgs();

            List<TriggerArtifactVariable> triggerArtifactVariables =
                workflowAction.getTriggerArgs()
                    .getTriggerArtifactVariables()
                    .stream()
                    .map(artifactVariable -> {
                      return fetchArtifactVariable(appId, artifactVariable, payLoadMap, serviceArtifactMapping);
                    })
                    .collect(toList());

            deploymentTrigger.setAction(
                WorkflowAction.builder()
                    .workflowId(workflowAction.getWorkflowId())
                    .workflowName(workflowAction.getWorkflowName())
                    .triggerArgs(TriggerArgs.builder()
                                     .triggerArtifactVariables(triggerArtifactVariables)
                                     .excludeHostsWithSameArtifact(triggerArgs.isExcludeHostsWithSameArtifact())
                                     .variables(triggerArgs.getVariables())
                                     .build())
                    .build());
          }
          break;
        default:
          unhandled(deploymentTrigger.getAction().getActionType());
      }
    }

    return null;
  }

  private Map<String, String> resolveWorkflowVariables(
      Map<String, Object> payLoadMap, DeploymentTrigger deploymentTrigger) {
    Map<String, String> variables = null;

    switch (deploymentTrigger.getAction().getActionType()) {
      case PIPELINE:
        PipelineAction pipelineAction = (PipelineAction) deploymentTrigger.getAction();
        if (pipelineAction.getTriggerArgs().getVariables() != null) {
          variables = updateWFVariables(pipelineAction.getTriggerArgs().getVariables(), payLoadMap);
        }
        break;
      case WORKFLOW:
        WorkflowAction workflowAction = (WorkflowAction) deploymentTrigger.getAction();
        if (workflowAction.getTriggerArgs().getVariables() != null) {
          variables = updateWFVariables(workflowAction.getTriggerArgs().getVariables(), payLoadMap);
        }
        break;
      default:
        unhandled(deploymentTrigger.getAction().getActionType());
    }

    return variables;
  }

  private Map<String, String> updateWFVariables(List<Variable> variables, Map<String, Object> payLoadMap) {
    if (isNotEmpty(variables)) {
      return variables.stream()
          .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
          .map(variable -> {
            String wfVariableValue = variable.getValue();
            if (ExpressionEvaluator.matchesVariablePattern(wfVariableValue)) {
              Object evalutedValue = expressionEvaluator.substitute(wfVariableValue, payLoadMap);
              if (evalutedValue != null) {
                variable.setValue(String.valueOf(evalutedValue));
              }
            }

            return variable;
          })
          .collect(Collectors.toMap(Variable::getName, Variable::getValue));
    } else {
      return new HashMap<>();
    }
  }

  private void validateWorkflowVariables(Map<String, String> variables) {
    List<String> missingWorkflowVariables =
        variables.keySet()
            .stream()
            .filter(variableName -> ExpressionEvaluator.matchesVariablePattern(variables.get(variableName)))
            .collect(toList());

    if (!missingWorkflowVariables.isEmpty()) {
      throw new TriggerException("some variables values are missing in payload " + missingWorkflowVariables, null);
    }
  }

  private String fetchEntityId(String appId, EntityType entityType, String entityName) {
    if (entityType == EntityType.SERVICE) {
      Service service = serviceResourceService.getServiceByName(appId, entityName);
      if (service == null) {
        throw new TriggerException("Service " + entityName + " does not exist ", null);
      }
      return serviceResourceService.getServiceByName(appId, entityName).getUuid();
    } else {
      throw new TriggerException("Entity type " + entityType + " is not supported ", null);
    }
  }

  private TriggerArtifactVariable fetchArtifactVariable(String appId, TriggerArtifactVariable triggerArtifactVariable,
      Map<String, Object> payLoadMap, Map<String, Map<String, ArtifactSummary>> serviceArtifactMapping) {
    logger.info("fetching trigger artifact variable for trigger {}  ", triggerArtifactVariable.getVariableName());
    String variableName = getSubstitutedValue(triggerArtifactVariable.getVariableName(), payLoadMap);

    String entityName = triggerArtifactVariable.getEntityName();
    EntityType entityType = triggerArtifactVariable.getEntityType();
    String entityId = triggerArtifactVariable.getEntityId();

    if (ExpressionEvaluator.matchesVariablePattern(triggerArtifactVariable.getEntityId())) {
      entityName = getSubstitutedValue(triggerArtifactVariable.getEntityId(), payLoadMap);
      entityType = triggerArtifactVariable.getEntityType(); // Can it be expression ?
      entityId = fetchEntityId(appId, EntityType.SERVICE, entityName);
    }

    TriggerArtifactVariable triggerArtifactVariableWithEntity =
        TriggerArtifactVariable.builder()
            .variableName(variableName)
            .entityId(entityId)
            .entityType(entityType)
            .entityName(entityName)
            .variableValue(triggerArtifactVariable.getVariableValue())
            .build();

    TriggerArtifactSelectionValue triggerArtifactSelectionValue = mapTriggerArtifactVariableToValue(
        appId, triggerArtifactVariableWithEntity, payLoadMap, variableName, serviceArtifactMapping);

    return TriggerArtifactVariable.builder()
        .variableName(variableName)
        .entityId(entityId)
        .entityType(entityType)
        .entityName(entityName)
        .variableValue(triggerArtifactSelectionValue)
        .build();
  }

  private TriggerArtifactSelectionValue mapTriggerArtifactVariableToValue(String appId,
      TriggerArtifactVariable triggerArtifactVariable, Map<String, Object> payLoadMap, String variableName,
      Map<String, Map<String, ArtifactSummary>> serviceArtifactMapping) {
    TriggerArtifactSelectionValue triggerArtifactVariableValue = triggerArtifactVariable.getVariableValue();

    switch (triggerArtifactVariableValue.getArtifactSelectionType()) {
      case LAST_COLLECTED:
        TriggerArtifactSelectionLastCollected lastCollected =
            (TriggerArtifactSelectionLastCollected) triggerArtifactVariableValue;

        String artifactServerId = lastCollected.getArtifactServerId();
        if (ExpressionEvaluator.matchesVariablePattern(artifactServerId)) {
          String serverName = getSubstitutedValue(lastCollected.getArtifactServerId(), payLoadMap);
          String accountId = appService.getAccountIdByAppId(appId);

          SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, serverName);
          if (settingAttribute != null) {
            artifactServerId = settingAttribute.getUuid();
          } else {
            throw new TriggerException(
                "Artifact server " + serverName + " does not exist for variable name " + variableName, USER);
          }
        }

        String artifactStreamId = lastCollected.getArtifactStreamId();
        if (ExpressionEvaluator.matchesVariablePattern(artifactStreamId)) {
          String streamName = getSubstitutedValue(lastCollected.getArtifactStreamId(), payLoadMap);
          ArtifactStream artifactStream =
              artifactStreamService.getArtifactStreamByName(appId, triggerArtifactVariable.getEntityId(), streamName);
          if (artifactStream != null) {
            artifactStreamId = artifactStream.getUuid();
          } else {
            throw new TriggerException(
                "Artifact stream " + streamName + " does not exist for variable name " + variableName, USER);
          }
        }

        String artifactFilter = getSubstitutedValue(lastCollected.getArtifactFilter(), payLoadMap);

        return TriggerArtifactSelectionLastCollected.builder()
            .artifactServerId(artifactServerId)
            .artifactStreamId(artifactStreamId)
            .artifactFilter(artifactFilter)
            .build();

      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed lastDeployed =
            (TriggerArtifactSelectionLastDeployed) triggerArtifactVariableValue;
        String executionId = lastDeployed.getId();
        TriggerLastDeployedType type = lastDeployed.getType();
        String name = lastDeployed.getName();

        if (ExpressionEvaluator.matchesVariablePattern(executionId)) {
          name = getSubstitutedValue(lastDeployed.getId(), payLoadMap);

          if (lastDeployed.getType() == TriggerLastDeployedType.WORKFLOW) {
            Workflow workflow = workflowService.readWorkflowByName(appId, name);
            if (workflow != null) {
              executionId = workflow.getUuid();
            } else {
              throw new TriggerException("Workflow with name  " + name + " does not exist", USER);
            }
            name = workflow.getName();
          } else if (lastDeployed.getType() == TriggerLastDeployedType.PIPELINE) {
            Pipeline pipeline = pipelineService.getPipelineByName(appId, name);
            if (pipeline != null) {
              executionId = pipeline.getUuid();
            } else {
              throw new TriggerException("Pipeline with name  " + name + " does not exist", USER);
            }
            name = pipeline.getName();
          }
        }
        return TriggerArtifactSelectionLastDeployed.builder().id(executionId).type(type).name(name).build();
      case WEBHOOK_VARIABLE:
        try {
          TriggerArtifactSelectionWebhook triggerArtifactSelectionWebhook =
              (TriggerArtifactSelectionWebhook) triggerArtifactVariableValue;

          String webhookArtifactServerId = triggerArtifactSelectionWebhook.getArtifactServerId();
          if (ExpressionEvaluator.matchesVariablePattern(webhookArtifactServerId)) {
            String serverName = getSubstitutedValue(triggerArtifactSelectionWebhook.getArtifactServerId(), payLoadMap);
            String accountId = appService.getAccountIdByAppId(appId);

            SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, serverName);
            if (settingAttribute != null) {
              webhookArtifactServerId = settingAttribute.getUuid();
            } else {
              throw new TriggerException("Artifact server " + serverName + " does not exist", USER);
            }
          }

          String webhookArtifactStreamId = triggerArtifactSelectionWebhook.getArtifactStreamId();
          if (ExpressionEvaluator.matchesVariablePattern(webhookArtifactStreamId)) {
            String streamName = getSubstitutedValue(triggerArtifactSelectionWebhook.getArtifactStreamId(), payLoadMap);
            ArtifactStream artifactStream =
                artifactStreamService.getArtifactStreamByName(webhookArtifactServerId, streamName);
            if (artifactStream != null) {
              webhookArtifactStreamId = artifactStream.getUuid();
            } else {
              throw new TriggerException("Artifact stream " + streamName + " does not exist", USER);
            }
          }

          String buildNumber = triggerArtifactSelectionWebhook.getArtifactFilter();
          if (ExpressionEvaluator.matchesVariablePattern(buildNumber) || isNotEmpty(serviceArtifactMapping)) {
            buildNumber = getSubstitutedValue(triggerArtifactSelectionWebhook.getArtifactFilter(), payLoadMap);

            logger.info("resolving build number for webhook trigger variable {}", variableName);
            if (isNotEmpty(serviceArtifactMapping)
                && serviceArtifactMapping.get(triggerArtifactVariable.getEntityId()) != null
                && serviceArtifactMapping.get(triggerArtifactVariable.getEntityId()).get(variableName) != null) {
              buildNumber =
                  serviceArtifactMapping.get(triggerArtifactVariable.getEntityId()).get(variableName).getBuildNo();

              logger.info("input build number in input map {}", buildNumber);
              if (isNotEmpty(serviceArtifactMapping)) {
                String artifactStreamName = serviceArtifactMapping.get(triggerArtifactVariable.getEntityId())
                                                .get(variableName)
                                                .getArtifactSourceName();

                logger.info("input artifactStreamName in input map {} entityId {}", artifactStreamName,
                    triggerArtifactVariable.getEntityId());

                if (artifactStreamName != null) {
                  // Artifact stream will have global app id after multi artifact
                  ArtifactStream artifactStream = artifactStreamService.getArtifactStreamByName(
                      appId, triggerArtifactVariable.getEntityId(), artifactStreamName);

                  if (artifactStream != null) {
                    webhookArtifactStreamId = artifactStream.getUuid();
                    webhookArtifactServerId = artifactStream.getSettingId();
                  } else {
                    throw new TriggerException(
                        "Artifact stream does not exist for stream name " + artifactStreamName, USER);
                  }
                }
              }
            }
          }
          return TriggerArtifactSelectionWebhook.builder()
              .artifactServerId(webhookArtifactServerId)
              .artifactStreamId(webhookArtifactStreamId)
              .artifactFilter(buildNumber)
              .build();
        } catch (Exception e) {
          logger.error("Failed to resolve buildNumber in input webhook request");
          throw e;
        }
      default:
        unhandled(triggerArtifactVariableValue.getArtifactSelectionType());
    }
    return null;
  }

  private void validateCustomPayloadExpressionFromInput(
      Map<String, Object> payLoadMap, WebhookCondition webhookCondition, DeploymentTrigger deploymentTrigger) {
    switch (webhookCondition.getPayloadSource().getType()) {
      case GITHUB:
        GitHubPayloadSource gitHubPayloadSource = (GitHubPayloadSource) webhookCondition.getPayloadSource();
        validateCustomPayloadExpressions(
            payLoadMap, gitHubPayloadSource.getCustomPayloadExpressions(), deploymentTrigger);
        break;
      case GITLAB:
        GitLabsPayloadSource gitLabsPayloadSource = (GitLabsPayloadSource) webhookCondition.getPayloadSource();
        validateCustomPayloadExpressions(
            payLoadMap, gitLabsPayloadSource.getCustomPayloadExpressions(), deploymentTrigger);
        break;
      case BITBUCKET:
        BitBucketPayloadSource bitBucketPayloadSource = (BitBucketPayloadSource) webhookCondition.getPayloadSource();
        validateCustomPayloadExpressions(
            payLoadMap, bitBucketPayloadSource.getCustomPayloadExpressions(), deploymentTrigger);
        break;
      case CUSTOM:
        break;
      default:
        throw new IllegalArgumentException("invalid type: " + webhookCondition.getPayloadSource().getType());
    }
  }

  private void validateStoredEventTypeFromInput(DeploymentTrigger deploymentTrigger, HttpHeaders httpHeaders,
      Map<String, Object> payLoadMap, WebhookCondition webhookCondition) {
    switch (webhookCondition.getPayloadSource().getType()) {
      case GITHUB:
        GitHubPayloadSource gitHubPayloadSource = (GitHubPayloadSource) webhookCondition.getPayloadSource();
        boolean otherEventPresent = gitHubPayloadSource.getGitHubEventTypes().stream().anyMatch(
            event -> event.getEventType() == OTHER || event.getEventType() == ANY);

        if (otherEventPresent) {
          return;
        }

        String gitHubEvent = httpHeaders == null ? null : httpHeaders.getHeaderString(X_GIT_HUB_EVENT);
        logger.info("X-GitHub-Event is {} ", gitHubEvent);
        if (gitHubEvent == null) {
          throw new TriggerException("Header [X-GitHub-Event] is missing", USER);
        }

        WebhookEventType webhookEventType = WebhookEventType.find(gitHubEvent);

        if (PULL_REQUEST == webhookEventType) {
          Object prAction = payLoadMap.get("action");

          boolean actionExists = gitHubPayloadSource.getGitHubEventTypes().stream().anyMatch(event
              -> event.getEventType().equals(GitHubEventType.PULL_REQUEST_ANY)
                  || event.getEventType().equals(GitHubEventType.find(prAction.toString())));

          if (!actionExists) {
            String msg = "Trigger [" + deploymentTrigger.getName()
                + "] is not associated with the received GitHub action [" + prAction + "]";
            throw new TriggerException(msg, USER);
          }
        }

        break;
      case GITLAB:
        return;
      case CUSTOM:
        return;
      case BITBUCKET:
        BitBucketPayloadSource bitBucketPayloadSource = (BitBucketPayloadSource) webhookCondition.getPayloadSource();

        boolean otherEventPresentInBitBucket =
            bitBucketPayloadSource.getBitBucketEvents().stream().anyMatch(event -> event.getEventType() == OTHER);

        if (otherEventPresentInBitBucket) {
          return;
        }

        logger.info("Trigger is set for BitBucket. Checking the http headers for the request type");
        String bitBucketEvent = httpHeaders == null ? null : httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT);
        logger.info("X-Event-Key is {} ", bitBucketEvent);
        if (bitBucketEvent == null) {
          throw new TriggerException("Header [X-Event-Key] is missing", USER);
        }

        BitBucketEventType bitBucketEventType = BitBucketEventType.find(bitBucketEvent);
        String errorMsg = "Trigger [" + deploymentTrigger.getName()
            + "] is not associated with the received BitBucket event [" + bitBucketEvent + "]";

        if (bitBucketPayloadSource.getBitBucketEvents() != null
            && (bitBucketPayloadSource.getBitBucketEvents().contains(bitBucketEventType)
                   || (BitBucketEventType.containsAllEvent(bitBucketPayloadSource.getBitBucketEvents())))) {
          return;
        } else {
          if (bitBucketEventType.getEventType() == PULL_REQUEST
              && (bitBucketPayloadSource.getBitBucketEvents().contains(PULL_REQUEST_ANY))) {
            return;
          }

          if (bitBucketEventType.getEventType() == ISSUE
              && (bitBucketPayloadSource.getBitBucketEvents().contains(ISSUE_ANY))) {
            return;
          }

          if (bitBucketEventType.getEventType() == PUSH
              && (bitBucketPayloadSource.getBitBucketEvents().contains(PUSH_ANY))) {
            return;
          }
          throw new TriggerException(errorMsg, USER);
        }
      default:
        throw new IllegalArgumentException("invalid type: " + webhookCondition.getPayloadSource().getType());
    }
  }

  private String getSubstitutedValue(String expression, Map<String, Object> payLoadMap) {
    Object evaluatedValue = expressionEvaluator.substitute(expression, payLoadMap);
    if (evaluatedValue != null) {
      return String.valueOf(evaluatedValue);
    }

    return expression;
  }

  private void validateCustomPayloadExpressions(Map<String, Object> payLoadMap,
      List<CustomPayloadExpression> customPayloadExpressions, DeploymentTrigger deploymentTrigger) {
    if (isNotEmpty(customPayloadExpressions)) {
      customPayloadExpressions.forEach(customPayloadExpression -> {
        String inputExpression = webhookEventUtils.findExpression(payLoadMap, customPayloadExpression.getExpression());
        validateValueWithRegex(customPayloadExpression.getValue(), inputExpression, deploymentTrigger);
      });
    }
  }

  private void validateValueWithRegex(
      String storedExpressionValue, String inputValue, DeploymentTrigger deploymentTrigger) {
    if (Pattern.compile(storedExpressionValue).matcher(inputValue).matches()) {
      return;
    }
    String msg = String.format("WebHook event value filter [%s] does not match with the trigger condition value [%s]",
        inputValue, storedExpressionValue);
    logger.error(msg + " appId {} triggerId {} ", deploymentTrigger.getAppId(), deploymentTrigger.getUuid());
    throw new TriggerException(msg, TriggerException.USER);
  }
}
