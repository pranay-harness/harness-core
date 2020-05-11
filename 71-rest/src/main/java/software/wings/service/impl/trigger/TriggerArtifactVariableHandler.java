package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.stream.Collectors.toList;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.TriggerArtifactSelectionFromPipelineSource;
import software.wings.beans.trigger.TriggerArtifactSelectionFromSource;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWebhook;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.TriggerLastDeployedType;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(CDC)
@Singleton
@ValidateOnExecution
@Slf4j
public class TriggerArtifactVariableHandler {
  @Inject private transient PipelineService pipelineService;
  @Inject private transient WorkflowService workflowService;
  @Inject private transient ServiceVariableService serviceVariablesService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient EnvironmentService environmentService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient SettingsService settingService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  public void validateTriggerArtifactVariables(String appId, List<TriggerArtifactVariable> triggerArtifactVariables) {
    if (triggerArtifactVariables != null) {
      triggerArtifactVariables.forEach(triggerArtifactVariable -> {
        String entityId = triggerArtifactVariable.getEntityId();
        if (entityId == null) {
          throw new TriggerException(
              "entityId is null for artifact variable " + triggerArtifactVariable.getVariableName(), null);
        }
        EntityType entityType = triggerArtifactVariable.getEntityType();

        if (entityType == null) {
          throw new TriggerException(
              "entityType is null for artifact variable " + triggerArtifactVariable.getVariableName(), null);
        }

        notNullCheck("Artifact value cannot be null for variable: " + triggerArtifactVariable.getVariableName(),
            triggerArtifactVariable.getVariableValue());
        validateTriggerArtifactSelection(
            appId, triggerArtifactVariable.getVariableValue(), triggerArtifactVariable.getVariableName());
      });
    }
  }

  public List<ArtifactVariable> fetchArtifactVariablesForExecution(
      String appId, DeploymentTrigger trigger, List<Artifact> artifacts) {
    List<ArtifactVariable> artifactVariables = new ArrayList<>();
    if (trigger.getAction() != null) {
      switch (trigger.getAction().getActionType()) {
        case PIPELINE:
          PipelineAction pipelineAction = (PipelineAction) trigger.getAction();
          Map<String, String> pipelineVariables = new HashMap<>();
          if (pipelineAction.getTriggerArgs() != null && pipelineAction.getTriggerArgs().getVariables() != null) {
            pipelineVariables = pipelineAction.getTriggerArgs()
                                    .getVariables()
                                    .stream()
                                    .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                                    .collect(Collectors.toMap(Variable::getName, Variable::getValue));
          }

          artifactVariables = pipelineService
                                  .fetchDeploymentMetadata(appId, pipelineAction.getPipelineId(), pipelineVariables,
                                      null, null, Include.ARTIFACT_SERVICE)
                                  .getArtifactVariables();

          if (artifactVariables != null && pipelineAction.getTriggerArgs() != null) {
            artifactVariables.forEach(artifactVariable -> {
              String value = fetchArtifactVariableValue(appId,
                  pipelineAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, trigger, artifacts);
              artifactVariable.setValue(value);
            });
          }
          break;
        case WORKFLOW:
          WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();
          Workflow workflow = workflowService.readWorkflow(appId, workflowAction.getWorkflowId());
          Map<String, String> variables = new HashMap<>();
          if (workflowAction.getTriggerArgs() != null && workflowAction.getTriggerArgs().getVariables() != null) {
            variables = workflowAction.getTriggerArgs()
                            .getVariables()
                            .stream()
                            .filter(variableEntry -> isNotEmpty(variableEntry.getValue()))
                            .collect(Collectors.toMap(Variable::getName, Variable::getValue));
          }
          artifactVariables =
              workflowService.fetchDeploymentMetadata(appId, workflow, variables, null, null, Include.ARTIFACT_SERVICE)
                  .getArtifactVariables();

          if (artifactVariables != null && workflowAction.getTriggerArgs() != null) {
            artifactVariables.forEach(artifactVariable -> {
              String value = fetchArtifactVariableValue(appId,
                  workflowAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, trigger, artifacts);
              artifactVariable.setValue(value);
            });
          }
          break;
        default:
          unhandled(trigger.getAction().getActionType());
      }
    }

    return artifactVariables;
  }

  public List<TriggerArtifactVariable> transformTriggerArtifactVariables(
      String appId, List<TriggerArtifactVariable> inputTriggerArtifactVariables) {
    if (inputTriggerArtifactVariables != null) {
      return inputTriggerArtifactVariables.stream()
          .map(triggerArtifactVariable
              -> TriggerArtifactVariable.builder()
                     .variableName(triggerArtifactVariable.getVariableName())
                     .variableValue(addTriggerVariableValue(appId, triggerArtifactVariable.getVariableValue()))
                     .entityId(triggerArtifactVariable.getEntityId())
                     .entityType(triggerArtifactVariable.getEntityType())
                     .entityName(fetchEntityName(
                         appId, triggerArtifactVariable.getEntityId(), triggerArtifactVariable.getEntityType()))
                     .build())
          .collect(toList());
    }
    return null;
  }

  public String fetchArtifactVariableValue(String appId, List<TriggerArtifactVariable> triggerArtifactVariables,
      ArtifactVariable artifactVariable, DeploymentTrigger trigger, List<Artifact> artifacts) {
    if (triggerArtifactVariables != null) {
      Optional<TriggerArtifactVariable> artifactVariableOpt =
          triggerArtifactVariables.stream()
              .filter(triggerArtifactVariable
                  -> triggerArtifactVariable.getEntityId().equals(artifactVariable.getEntityId())
                      && artifactVariable.getName().equals(triggerArtifactVariable.getVariableName()))
              .findFirst();

      if (artifactVariableOpt.isPresent()) {
        TriggerArtifactVariable triggerArtifactVariable = artifactVariableOpt.get();
        return mapTriggerArtifactVariableToValue(appId, triggerArtifactVariable, artifacts, trigger, artifactVariable);
      } else {
        throw new WingsException("artifact variable " + artifactVariable.getName() + " does not exist");
      }
    }
    return null;
  }

  private TriggerArtifactSelectionValue addTriggerVariableValue(
      String appId, TriggerArtifactSelectionValue inputValue) {
    if (inputValue == null) {
      return null;
    }

    switch (inputValue.getArtifactSelectionType()) {
      case LAST_COLLECTED:
        TriggerArtifactSelectionLastCollected artifactVar = (TriggerArtifactSelectionLastCollected) inputValue;
        String artifactStreamId = artifactVar.getArtifactStreamId();

        if (isExpression(artifactStreamId) && !isExpression(artifactVar.getArtifactServerId())) {
          SettingAttribute settingAttribute = settingService.get(artifactVar.getArtifactServerId());
          return TriggerArtifactSelectionLastCollected.builder()
              .artifactFilter(artifactVar.getArtifactFilter())
              .artifactStreamId(artifactVar.getArtifactStreamId())
              .artifactStreamName(artifactStreamId)
              .artifactServerId(artifactVar.getArtifactServerId())
              .artifactServerName(settingAttribute.getName())
              .build();
        } else if (isExpression(artifactStreamId) && isExpression(artifactVar.getArtifactServerId())) {
          return TriggerArtifactSelectionLastCollected.builder()
              .artifactFilter(artifactVar.getArtifactFilter())
              .artifactStreamId(artifactVar.getArtifactStreamId())
              .artifactStreamName(artifactVar.getArtifactStreamId())
              .artifactServerId(artifactVar.getArtifactServerId())
              .artifactServerName(artifactVar.getArtifactServerId())
              .build();
        } else if (!isExpression(artifactStreamId) && !isExpression(artifactVar.getArtifactServerId())) {
          ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
          SettingAttribute settingAttribute = settingService.get(artifactStream.getSettingId());
          return TriggerArtifactSelectionLastCollected.builder()
              .artifactFilter(artifactVar.getArtifactFilter())
              .artifactStreamId(artifactVar.getArtifactStreamId())
              .artifactServerId(artifactVar.getArtifactServerId())
              .artifactStreamName(artifactStream.getName())
              .artifactStreamType(artifactStream.getArtifactStreamType())
              .artifactServerName(settingAttribute.getName())
              .build();
        } else {
          throw new WingsException("artifact stream should also be expression if artifact server is expression");
        }
      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed workflowVar = (TriggerArtifactSelectionLastDeployed) inputValue;

        if (!isExpression(workflowVar.getId())) {
          return TriggerArtifactSelectionLastDeployed.builder()
              .id(workflowVar.getId())
              .name(fetchName(appId, workflowVar.getId(), workflowVar.getType()))
              .type(workflowVar.getType())
              .build();
        } else {
          return TriggerArtifactSelectionLastDeployed.builder()
              .id(workflowVar.getId())
              .name(workflowVar.getId())
              .type(workflowVar.getType())
              .build();
        }

      case ARTIFACT_SOURCE:
        return TriggerArtifactSelectionFromSource.builder().build();
      case PIPELINE_SOURCE:
        return TriggerArtifactSelectionFromPipelineSource.builder().build();
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook artifactVarWebhook = (TriggerArtifactSelectionWebhook) inputValue;
        String artifactStreamIdWebhook = artifactVarWebhook.getArtifactStreamId();

        if (isExpression(artifactStreamIdWebhook) && !isExpression(artifactVarWebhook.getArtifactServerId())) {
          SettingAttribute settingAttribute = settingService.get(artifactVarWebhook.getArtifactServerId());
          return TriggerArtifactSelectionWebhook.builder()
              .artifactStreamId(artifactVarWebhook.getArtifactStreamId())
              .artifactStreamName(artifactVarWebhook.getArtifactStreamId())
              .artifactServerId(artifactVarWebhook.getArtifactServerId())
              .artifactFilter(artifactVarWebhook.getArtifactFilter())
              .artifactServerName(settingAttribute.getName())
              .build();
        } else if (isExpression(artifactStreamIdWebhook) && isExpression(artifactVarWebhook.getArtifactServerId())) {
          return TriggerArtifactSelectionWebhook.builder()
              .artifactStreamId(artifactVarWebhook.getArtifactStreamId())
              .artifactStreamName(artifactVarWebhook.getArtifactStreamId())
              .artifactServerId(artifactVarWebhook.getArtifactServerId())
              .artifactServerName(artifactVarWebhook.getArtifactServerId())
              .artifactFilter(artifactVarWebhook.getArtifactFilter())
              .build();
        } else if (!isExpression(artifactStreamIdWebhook) && !isExpression(artifactVarWebhook.getArtifactServerId())) {
          ArtifactStream artifactStream = artifactStreamService.get(artifactStreamIdWebhook);
          SettingAttribute settingAttribute = settingService.get(artifactStream.getSettingId());
          return TriggerArtifactSelectionWebhook.builder()
              .artifactStreamId(artifactVarWebhook.getArtifactStreamId())
              .artifactServerId(artifactVarWebhook.getArtifactServerId())
              .artifactStreamName(artifactStream.getName())
              .artifactStreamType(artifactStream.getArtifactStreamType())
              .artifactServerName(settingAttribute.getName())
              .artifactFilter(artifactVarWebhook.getArtifactFilter())
              .build();
        } else {
          throw new WingsException("artifact stream should also be expression if artifact server is expression");
        }
      default:
        unhandled(inputValue.getArtifactSelectionType());
    }

    return null;
  }

  private String fetchName(String appId, String workflowId, TriggerLastDeployedType triggerLastDeployedType) {
    switch (triggerLastDeployedType) {
      case WORKFLOW:
        return workflowService.fetchWorkflowName(appId, workflowId);
      case PIPELINE:
        return pipelineService.fetchPipelineName(appId, workflowId);
      default:
        unhandled(triggerLastDeployedType);
    }

    return null;
  }

  private String fetchEntityName(String appId, String entityId, EntityType entityType) {
    if (isExpression(entityId)) {
      return entityId;
    }
    switch (entityType) {
      case SERVICE:
        return serviceResourceService.get(entityId).getName();
      case ENVIRONMENT:
        return environmentService.get(appId, entityId, false).getName();
      case WORKFLOW:
        return workflowService.fetchWorkflowName(appId, entityId);
      default:
        unhandled(entityType);
    }

    return null;
  }

  private boolean isExpression(String inputValue) {
    return matchesVariablePattern(inputValue);
  }

  private void validateTriggerArtifactSelection(
      String appId, TriggerArtifactSelectionValue variableValue, String variableName) {
    switch (variableValue.getArtifactSelectionType()) {
      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed workflowVar = (TriggerArtifactSelectionLastDeployed) variableValue;

        if (workflowVar.getId() == null) {
          throw new TriggerException("Workflow/Pipeline Id is null for artifact variable " + variableName, null);
        }
        if (isExpression(workflowVar.getId())) {
          return;
        }
        try {
          fetchName(appId, workflowVar.getId(), workflowVar.getType());
        } catch (WingsException exception) {
          throw new TriggerException("Workflow/Pipeline does not exist for variable " + variableName, null);
        }
        break;
      case ARTIFACT_SOURCE:
        break;
      case PIPELINE_SOURCE:
        break;
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook artifactSelectionWebhook = (TriggerArtifactSelectionWebhook) variableValue;

        if (artifactSelectionWebhook.getArtifactServerId() == null) {
          throw new TriggerException("artifact server Id is null for artifact variable " + variableName, null);
        }

        if (artifactSelectionWebhook.getArtifactStreamId() == null) {
          throw new TriggerException("artifact stream Id is null for artifact variable " + variableName, null);
        }

        if (isExpression(artifactSelectionWebhook.getArtifactServerId())
            && !isExpression(artifactSelectionWebhook.getArtifactStreamId())) {
          throw new WingsException("artifact stream should also be expression if artifact server is expression");
        }
        if (isExpression(artifactSelectionWebhook.getArtifactStreamId())) {
          return;
        }
        ArtifactStream artifactStreamWebhook =
            artifactStreamService.get(artifactSelectionWebhook.getArtifactStreamId());
        notNullCheck("artifactStream does not exist for id " + artifactSelectionWebhook.getArtifactStreamId(),
            artifactStreamWebhook);
        break;
      case LAST_COLLECTED:
        TriggerArtifactSelectionLastCollected artifactVar = (TriggerArtifactSelectionLastCollected) variableValue;

        if (artifactVar.getArtifactServerId() == null) {
          throw new TriggerException("artifact server Id is null for artifact variable " + variableName, null);
        }

        if (artifactVar.getArtifactStreamId() == null) {
          throw new TriggerException("artifact stream Id is null for artifact variable " + variableName, null);
        }

        if (isExpression(artifactVar.getArtifactServerId()) && !isExpression(artifactVar.getArtifactStreamId())) {
          throw new WingsException(
              "artifact stream should also be expression also be expression if artifact server is expression");
        }
        if (isExpression(artifactVar.getArtifactStreamId())) {
          return;
        }
        ArtifactStream artifactStream = artifactStreamService.get(artifactVar.getArtifactStreamId());
        notNullCheck("artifactStream does not exist for id " + artifactVar.getArtifactStreamId(), artifactStream);
        break;
      default:
        unhandled(variableValue.getArtifactSelectionType());
    }
  }

  private void validateVariableName(String appId, String entityId, EntityType entityType, String variableName) {
    if (isExpression(variableName) || isExpression(entityId)) {
      return;
    }

    if (variableName == null) {
      throw new TriggerException("trigger artifact variable name is null", null);
    }

    switch (entityType) {
      case SERVICE:
      case SERVICE_TEMPLATE:
      case ENVIRONMENT:
        List<ServiceVariable> serviceVariables =
            serviceVariablesService.getServiceVariablesForEntity(appId, entityId, OBTAIN_VALUE);
        boolean variableExists =
            serviceVariables.stream().anyMatch(serviceVariable -> serviceVariable.getName().equals(variableName));
        if (!variableExists) {
          throw new WingsException("variable name " + variableName + " does not exist");
        }

        break;
      case WORKFLOW:
        List<Variable> variables =
            workflowService.readWorkflow(appId, entityId).getOrchestrationWorkflow().getUserVariables();
        boolean wfVariableExists = variables.stream().anyMatch(
            variable -> variable.getName().equals(variableName) && variable.getType() == VariableType.ARTIFACT);
        if (!wfVariableExists) {
          throw new WingsException("Variable name " + variableName + " does not exist ");
        }
        break;
      default:
        unhandled(entityType);
    }
  }

  private String mapTriggerArtifactVariableToValue(String appId, TriggerArtifactVariable triggerArtifactVariable,
      List<Artifact> artifacts, DeploymentTrigger trigger, ArtifactVariable artifactVariable) {
    switch (triggerArtifactVariable.getVariableValue().getArtifactSelectionType()) {
      case LAST_COLLECTED:
        TriggerArtifactSelectionLastCollected triggerArtifactVariableValueArtifact =
            (TriggerArtifactSelectionLastCollected) triggerArtifactVariable.getVariableValue();

        return fetchArtifactUuid(triggerArtifactVariableValueArtifact.getArtifactStreamId(),
            triggerArtifactVariableValueArtifact.getArtifactFilter(), true, artifactVariable);
      case ARTIFACT_SOURCE:
        return artifacts.get(artifacts.size() - 1).getUuid();
      case PIPELINE_SOURCE:
        return fetchLastDeployedArtifacts(
            appId, ((PipelineAction) trigger.getAction()).getPipelineId(), triggerArtifactVariable);
      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed triggerArtifactSelectionLastDeployed =
            (TriggerArtifactSelectionLastDeployed) triggerArtifactVariable.getVariableValue();
        return fetchLastDeployedArtifacts(appId, triggerArtifactSelectionLastDeployed.getId(), triggerArtifactVariable);
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook artifactSelectionWebhook =
            (TriggerArtifactSelectionWebhook) triggerArtifactVariable.getVariableValue();
        return fetchArtifactUuid(artifactSelectionWebhook.getArtifactStreamId(),
            artifactSelectionWebhook.getArtifactFilter(), false, artifactVariable);
      default:
        unhandled(triggerArtifactVariable.getVariableValue().getArtifactSelectionType());
    }
    return null;
  }

  private String fetchArtifactUuid(
      String artifactStreamId, String artifactFilter, boolean regex, ArtifactVariable artifactVariable) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (!artifactVariable.getAllowedList().contains(artifactStream.getUuid())) {
      throw new WingsException("artifact stream" + artifactStream.getName() + " does not exist in artifactVariable "
          + artifactVariable.getName() + " allowed list");
    }
    notNullCheck("ArtifactStream was deleted", artifactStream, USER);
    Artifact lastCollectedArtifact;
    if (isEmpty(artifactFilter)) {
      lastCollectedArtifact = artifactService.fetchLastCollectedArtifact(artifactStream);
      if (lastCollectedArtifact != null) {
        return lastCollectedArtifact.getUuid();
      } else {
        throw new WingsException("No artifact exist for trigger with artifact source " + artifactStream.getName());
      }
    } else {
      lastCollectedArtifact = artifactService.getArtifactByBuildNumber(artifactStream, artifactFilter, regex);
      if (lastCollectedArtifact != null) {
        return lastCollectedArtifact.getUuid();
      } else {
        throw new WingsException("No artifact exist for trigger with artifact source " + artifactStream.getName()
            + "Build filter: " + artifactFilter);
      }
    }
  }

  private String fetchLastDeployedArtifacts(
      String appId, String workflowId, TriggerArtifactVariable triggerArtifactVariable) {
    List<ArtifactVariable> lastDeployedArtifactVariables =
        workflowExecutionService.obtainLastGoodDeployedArtifactsVariables(appId, workflowId);

    if (isEmpty(lastDeployedArtifactVariables)) {
      logger.warn(
          "Last deployed workflow/Pipeline {} does not have last deployed artifacts for app {}", workflowId, appId);

      List<Artifact> artifacts = getLastDeployedArtifacts(appId, workflowId, triggerArtifactVariable.getEntityId());
      logger.warn("More than one artifacts found for service {} for app {}, picking first one",
          triggerArtifactVariable.getEntityId(), appId);
      if (EmptyPredicate.isNotEmpty(artifacts)) {
        return artifacts.get(0).getUuid();
      }
    } else {
      Optional<ArtifactVariable> artifactVariable =
          lastDeployedArtifactVariables.stream()
              .filter(lastDeployedArtifact -> {
                return triggerArtifactVariable.getEntityId().equals(lastDeployedArtifact.getEntityId())
                    && triggerArtifactVariable.getVariableName().equals(lastDeployedArtifact.getName());
              })
              .findFirst();

      if (artifactVariable.isPresent()) {
        return artifactVariable.get().getValue();
      } else {
        throw new WingsException("selected workflow/Pipeline " + workflowId + " does not have variable name "
            + triggerArtifactVariable.getVariableName() + " for trigger ");
      }
    }

    return null;
  }

  private List<Artifact> getLastDeployedArtifacts(String appId, String workflowId, String serviceId) {
    List<Artifact> lastDeployedArtifacts = workflowExecutionService.obtainLastGoodDeployedArtifacts(appId, workflowId);
    if (lastDeployedArtifacts != null && serviceId != null) {
      List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(appId, serviceId);
      if (isEmpty(artifactStreamIds)) {
        logger.warn("serviceId does not have artifact streams for app {}", serviceId, appId);
        lastDeployedArtifacts = new ArrayList<>();
      } else {
        lastDeployedArtifacts = lastDeployedArtifacts.stream()
                                    .filter(artifact -> artifactStreamIds.contains(artifact.getArtifactStreamId()))
                                    .collect(toList());
      }
    }
    return lastDeployedArtifacts == null ? new ArrayList<>() : lastDeployedArtifacts;
  }
}
