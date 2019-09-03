package software.wings.service.impl.trigger;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.toList;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
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
import software.wings.beans.trigger.WorkflowAction;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

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

  public void validateTriggerArtifactVariables(String appId, List<TriggerArtifactVariable> triggerArtifactVariables) {
    if (triggerArtifactVariables != null) {
      triggerArtifactVariables.forEach(triggerArtifactVariable -> {
        String entityId = triggerArtifactVariable.getEntityId();
        EntityType entityType = triggerArtifactVariable.getEntityType();
        validateVariableName(appId, entityId, entityType, triggerArtifactVariable.getVariableName());
        if (triggerArtifactVariable.getVariableValue() != null) {
          validateTriggerArtifactSelection(
              appId, triggerArtifactVariable.getVariableValue(), triggerArtifactVariable.getVariableName());
        }
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
          Pipeline pipeline = pipelineService.readPipeline(appId, pipelineAction.getPipelineId(), false);
          artifactVariables =
              pipelineService.fetchDeploymentMetadata(appId, pipeline, null, null, Include.ARTIFACT_SERVICE)
                  .getArtifactVariables();

          if (artifactVariables != null && pipelineAction.getTriggerArgs() != null) {
            artifactVariables.forEach(artifactVariable -> {
              String value = fetchArtifactVariableValue(appId,
                  pipelineAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, trigger, artifacts);
              artifactVariable.setValue(value);
            });
          }
          break;
        case ORCHESTRATION:
          WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();
          Workflow workflow = workflowService.readWorkflow(appId, workflowAction.getWorkflowId());
          Map<String, String> variables = null;
          if (workflowAction.getTriggerArgs() != null && workflowAction.getTriggerArgs().getVariables() != null) {
            variables = workflowAction.getTriggerArgs().getVariables().stream().collect(
                Collectors.toMap(Variable::getName, Variable::getValue));
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
          .map(triggerArtifactVariable -> {
            return TriggerArtifactVariable.builder()
                .variableName(triggerArtifactVariable.getVariableName())
                .variableValue(addTriggerVariableValue(appId, triggerArtifactVariable.getVariableValue()))
                .entityId(triggerArtifactVariable.getEntityId())
                .entityType(triggerArtifactVariable.getEntityType())
                .entityName(fetchEntityName(
                    appId, triggerArtifactVariable.getEntityId(), triggerArtifactVariable.getEntityType()))
                .build();
          })
          .collect(toList());
    } else {
      return null;
    }
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
              .artifactServerId(artifactVar.getArtifactServerId())
              .artifactServerName(settingAttribute.getName())
              .build();
        } else if (isExpression(artifactStreamId) && isExpression(artifactVar.getArtifactServerId())) {
          return TriggerArtifactSelectionLastCollected.builder()
              .artifactFilter(artifactVar.getArtifactFilter())
              .artifactStreamId(artifactVar.getArtifactStreamId())
              .artifactServerId(artifactVar.getArtifactServerId())
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

        if (!isExpression(workflowVar.getWorkflowId())) {
          return TriggerArtifactSelectionLastDeployed.builder()
              .workflowId(workflowVar.getWorkflowId())
              .workflowName(fetchWorkflowName(appId, workflowVar.getWorkflowId()))
              .build();
        } else {
          return TriggerArtifactSelectionLastDeployed.builder().workflowId(workflowVar.getWorkflowId()).build();
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
              .artifactServerId(artifactVarWebhook.getArtifactServerId())
              .artifactServerName(settingAttribute.getName())
              .build();
        } else if (isExpression(artifactStreamIdWebhook) && isExpression(artifactVarWebhook.getArtifactServerId())) {
          return TriggerArtifactSelectionWebhook.builder()
              .artifactStreamId(artifactVarWebhook.getArtifactStreamId())
              .artifactServerId(artifactVarWebhook.getArtifactServerId())
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
              .build();
        } else {
          throw new WingsException("artifact stream should also be expression if artifact server is expression");
        }
      default:
        unhandled(inputValue.getArtifactSelectionType());
    }

    return null;
  }

  private String fetchWorkflowName(String appId, String workflowId) {
    return workflowService.fetchWorkflowName(appId, workflowId);
  }

  private String fetchEntityName(String appId, String entityId, EntityType entityType) {
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

        if (isExpression(workflowVar.getWorkflowId())) {
          return;
        }
        try {
          workflowService.fetchWorkflowName(appId, workflowVar.getWorkflowId());
        } catch (WingsException exception) {
          throw new WingsException("workflow does not exist for variable " + variableName);
        }
        break;
      case ARTIFACT_SOURCE:
        break;
      case PIPELINE_SOURCE:
        break;
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook artifactSelectionWebhook = (TriggerArtifactSelectionWebhook) variableValue;
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
            variable -> variable.getName().equals(variableName) && variable.getType().equals(VariableType.ARTIFACT));
        if (!wfVariableExists) {
          throw new WingsException("Variable name " + variableName + " does not exist ");
        }
        break;
      default:
        unhandled(entityType);
    }
  }

  private String fetchArtifactVariableValue(String appId, List<TriggerArtifactVariable> triggerArtifactVariables,
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

  private String mapTriggerArtifactVariableToValue(String appId, TriggerArtifactVariable triggerArtifactVariable,
      List<Artifact> artifacts, DeploymentTrigger trigger, ArtifactVariable artifactVariable) {
    switch (triggerArtifactVariable.getVariableValue().getArtifactSelectionType()) {
      case LAST_COLLECTED:
        TriggerArtifactSelectionLastCollected triggerArtifactVariableValueArtifact =
            (TriggerArtifactSelectionLastCollected) triggerArtifactVariable.getVariableValue();

        return fetchArtifactUuid(triggerArtifactVariableValueArtifact.getArtifactStreamId(),
            triggerArtifactVariableValueArtifact.getArtifactFilter(), true, artifactVariable);
      case ARTIFACT_SOURCE:
        return artifacts.stream().findFirst().get().getUuid();
      case PIPELINE_SOURCE:
        return fetchLastDeployedArtifacts(
            appId, ((PipelineAction) trigger.getAction()).getPipelineId(), triggerArtifactVariable.getVariableName());
      case LAST_DEPLOYED:
        TriggerArtifactSelectionLastDeployed triggerArtifactSelectionLastDeployed =
            (TriggerArtifactSelectionLastDeployed) triggerArtifactVariable.getVariableValue();
        return fetchLastDeployedArtifacts(
            appId, triggerArtifactSelectionLastDeployed.getWorkflowId(), triggerArtifactVariable.getVariableName());
      case WEBHOOK_VARIABLE:
        TriggerArtifactSelectionWebhook artifactSelectionWebhook =
            (TriggerArtifactSelectionWebhook) triggerArtifactVariable.getVariableValue();
        return fetchArtifactUuid(artifactSelectionWebhook.getArtifactStreamId(),
            artifactSelectionWebhook.getBuildNumber(), false, artifactVariable);
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
        throw new WingsException("No artifact exist for trigger with artifact source " + artifactStream.getName());
      }
    }
  }

  private String fetchLastDeployedArtifacts(String appId, String workflowId, String variableName) {
    List<ArtifactVariable> lastDeployedArtifacts =
        workflowExecutionService.obtainLastGoodDeployedArtifactsVariables(appId, workflowId);

    if (isEmpty(lastDeployedArtifacts)) {
      logger.warn(
          "Last deployed workflow/Pipeline {} does not have last deployed artifacts for app {}", workflowId, appId);
    }
    Optional<ArtifactVariable> artifactVariable =
        lastDeployedArtifacts.stream()
            .filter(lastDeployedArtifact -> { return variableName.equals(lastDeployedArtifact.getName()); })
            .findFirst();

    if (artifactVariable.isPresent()) {
      return artifactVariable.get().getValue();
    } else {
      throw new WingsException("selected workflow/Pipeline " + workflowId + " does not have variable name "
          + variableName + " for trigger ");
    }
  }
}
