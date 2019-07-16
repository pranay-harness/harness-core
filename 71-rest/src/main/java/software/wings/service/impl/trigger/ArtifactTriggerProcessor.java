package software.wings.service.impl.trigger;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.trigger.TriggerArtifactSelectionValue.ArtifactVariableType.ORCHESTRATION;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.Service;
import software.wings.beans.VariableType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerArtifactSelectionArtifact;
import software.wings.beans.trigger.TriggerArtifactSelectionValue;
import software.wings.beans.trigger.TriggerArtifactSelectionWorkflow;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ArtifactTriggerProcessor implements TriggerProcessor {
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient ExecutorService executorService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient WorkflowService workflowService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient PipelineService pipelineService;
  @Override
  public void validateTriggerCondition(DeploymentTrigger trigger) {
    // TODO: ASR: update when index added on setting_id + name
    ArtifactCondition artifactCondition = (ArtifactCondition) trigger.getCondition();

    ArtifactStream artifactStream = artifactStreamService.get(artifactCondition.getArtifactStreamId());
    notNullCheck("Artifact Source is mandatory for New Artifact Condition Trigger", artifactStream, USER);

    Service service =
        artifactStreamServiceBindingService.getService(trigger.getAppId(), artifactStream.getUuid(), false);
    notNullCheck("Service associated to the artifact source [" + artifactStream.getSourceName() + "] does not exist",
        service, USER);

    trigger.setCondition(ArtifactCondition.builder()
                             .artifactStreamId(artifactCondition.getArtifactStreamId())
                             .artifactSourceName(artifactStream.getSourceName())
                             .artifactFilter(artifactCondition.getArtifactFilter())
                             .build());
  }

  @Override
  public void updateTriggerCondition(DeploymentTrigger deploymentTrigger) {
    ArtifactCondition artifactCondition = (ArtifactCondition) deploymentTrigger.getCondition();
    ArtifactStream artifactStream = artifactStreamService.get(artifactCondition.getArtifactStreamId());

    deploymentTrigger.setCondition(ArtifactCondition.builder()
                                       .artifactStreamId(artifactCondition.getArtifactStreamId())
                                       .artifactSourceName(artifactStream.getSourceName())
                                       .artifactStreamType(artifactStream.getArtifactStreamType())
                                       .artifactFilter(artifactCondition.getArtifactFilter())
                                       .build());
  }

  public void triggerExecutionPostArtifactCollection(
      String appId, String artifactStreamId, List<Artifact> collectedArtifacts) {
    executorService.execute(() -> executeTrigger(appId, artifactStreamId, collectedArtifacts));
  }

  private void executeTrigger(String appId, String artifactStreamId, List<Artifact> collectedArtifacts) {
    triggerServiceHelper.getNewArtifactTriggers(appId, artifactStreamId).forEach(trigger -> {
      logger.info("Trigger found with name {} and Id {} for artifactStreamId {}", trigger.getName(), trigger.getUuid(),
          artifactStreamId);
      ArtifactCondition artifactTriggerCondition = (ArtifactCondition) trigger.getCondition();
      List<Artifact> artifacts = new ArrayList<>();
      if (isEmpty(artifactTriggerCondition.getArtifactFilter())) {
        Artifact lastCollected = collectedArtifacts.get(collectedArtifacts.size() - 1);
        logger.info("No artifact filter set. Triggering with the collected artifact {}", lastCollected.getUuid());
        artifacts.add(lastCollected);
      } else {
        addArtifactForRegex(trigger, collectedArtifacts, artifactTriggerCondition, artifacts);
      }
      if (isEmpty(artifacts)) {
        logger.warn(
            "Skipping execution - artifact does not match with the given filter. So, skipping the complete deployment {}",
            artifactTriggerCondition);
        return;
      }

      executeDeployment(trigger, artifacts, artifactStreamId);
    });
  }

  private void addArtifactForRegex(DeploymentTrigger trigger, List<Artifact> collectedArtifacts,
      ArtifactCondition artifactTriggerCondition, List<Artifact> artifacts) {
    logger.info("Artifact filter {} set. Going over all the artifacts to find the matched artifacts",
        artifactTriggerCondition.getArtifactFilter());
    List<Artifact> matchedArtifacts =
        collectedArtifacts.stream()
            .filter(artifact
                -> triggerServiceHelper.checkArtifactMatchesArtifactFilter(
                    trigger.getUuid(), artifact, artifactTriggerCondition.getArtifactFilter()))
            .collect(Collectors.toList());
    if (isNotEmpty(matchedArtifacts)) {
      logger.info(
          "Matched artifacts {}. Selecting the latest artifact", matchedArtifacts.get(matchedArtifacts.size() - 1));
      artifacts.add(matchedArtifacts.get(matchedArtifacts.size() - 1));
    } else {
      logger.info("Artifacts {} not matched with the given artifact filter", artifacts);
    }
  }
  private void executeDeployment(DeploymentTrigger trigger, List<Artifact> artifacts, String artifactStreamId) {
    if (trigger.getAction() != null) {
      logger.info("Artifact selections found collecting artifacts as per artifactStream selections");

      List<ArtifactVariable> artifactVariables =
          fetchArtifactVariablesFromSelection(trigger.getAppId(), trigger, artifacts);

      logger.info("The artifact variables  set for the trigger {} are {}", trigger.getUuid(),
          artifactVariables.stream().map(ArtifactVariable::getName).collect(toList()));
      try {
        triggerDeployment(artifactVariables, trigger, null);
      } catch (WingsException exception) {
        exception.addContext(Application.class, trigger.getAppId());
        exception.addContext(ArtifactStream.class, artifactStreamId);
        exception.addContext(Trigger.class, trigger.getUuid());
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      }
    } else {
      logger.info("No Artifacts matched. Hence Skipping the deployment");
      return;
    }
  }

  public void triggerDeployment(
      List<ArtifactVariable> artifactVariables, DeploymentTrigger trigger, TriggerExecution triggerExecution) {
    workflowExecutionService.triggerPipelineExecution(trigger.getAppId(), "", null, null);
  }

  private List<ArtifactVariable> fetchArtifactVariablesFromSelection(
      String appId, DeploymentTrigger trigger, List<Artifact> artifacts) {
    List<ArtifactVariable> artifactVariables = new ArrayList<>();
    if (trigger.getAction() != null) {
      switch (trigger.getAction().getActionType()) {
        case PIPELINE:
          PipelineAction pipelineAction = (PipelineAction) trigger.getAction();

          artifactVariables = pipelineService.readPipeline(appId, pipelineAction.getPipelineId(), false)
                                  .getPipelineVariables()
                                  .stream()
                                  .filter(variable -> { return variable.getType().equals(VariableType.ARTIFACT); })
                                  .map(variable -> { return (ArtifactVariable) variable; })
                                  .collect(Collectors.toList());

          artifactVariables.forEach(artifactVariable -> {
            String value = fetchArtifactVariableValue(appId,
                pipelineAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, trigger, artifacts);
            artifactVariable.setValue(value);
          });
          break;
        case ORCHESTRATION:
          WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();

          artifactVariables = workflowService.readWorkflow(appId, workflowAction.getWorkflowId())
                                  .getOrchestrationWorkflow()
                                  .getUserVariables()
                                  .stream()
                                  .filter(variable -> { return variable.getType().equals(VariableType.ARTIFACT); })
                                  .map(variable -> { return (ArtifactVariable) variable; })
                                  .collect(Collectors.toList());

          artifactVariables.forEach(artifactVariable -> {
            String value = fetchArtifactVariableValue(appId,
                workflowAction.getTriggerArgs().getTriggerArtifactVariables(), artifactVariable, trigger, artifacts);
            artifactVariable.setValue(value);
          });
          break;
        default:
          unhandled(trigger.getAction().getActionType());
      }
    }

    return artifactVariables;
  }

  private String fetchArtifactVariableValue(String appId, List<TriggerArtifactVariable> triggerArtifactVariables,
      ArtifactVariable artifactVariable, DeploymentTrigger trigger, List<Artifact> artifacts) {
    Optional<TriggerArtifactVariable> artifactVariableOpt =
        triggerArtifactVariables.stream()
            .filter(triggerArtifactVariable
                -> triggerArtifactVariable.getEntityId().equals(artifactVariable.getEntityId())
                    && artifactVariable.getName().equals(triggerArtifactVariable.getVariableName()))
            .findFirst();

    if (artifactVariableOpt.isPresent()) {
      TriggerArtifactVariable triggerArtifactVariable = artifactVariableOpt.get();
      return mapTriggerArtifactVariableToValue(appId, triggerArtifactVariable, artifacts);
    } else {
      throw new WingsException("artifact variable " + artifactVariable.getName() + " not exist");
    }
  }

  private String mapTriggerArtifactVariableToValue(
      String appId, TriggerArtifactVariable triggerArtifactVariable, List<Artifact> artifacts) {
    switch (triggerArtifactVariable.getType()) {
      case LAST_COLLECTED:
        TriggerArtifactSelectionArtifact triggerArtifactVariableValueArtifact =
            (TriggerArtifactSelectionArtifact) triggerArtifactVariable.getVariableValue();

        triggerArtifactVariableValueArtifact.getArtifactStreamId();
        return fetchLastCollectedArtifact(triggerArtifactVariableValueArtifact.getArtifactStreamId(),
            triggerArtifactVariableValueArtifact.getArtifactFilter());
      case ARTIFACT_SOURCE:
        return artifacts.stream().findFirst().get().getUuid();
      case LAST_DEPLOYED:
        TriggerArtifactSelectionValue triggerArtifactVariableValue = triggerArtifactVariable.getVariableValue();
        if (triggerArtifactVariableValue.getArtifactVariableType() == ORCHESTRATION) {
          TriggerArtifactSelectionWorkflow triggerArtifactSelectionWorkflow =
              (TriggerArtifactSelectionWorkflow) triggerArtifactVariable.getVariableValue();
          return getLastDeployedArtifacts(appId, triggerArtifactSelectionWorkflow.getWorkflowId(),
              triggerArtifactSelectionWorkflow.getVariableName());
        } else {
          throw new WingsException(
              triggerArtifactVariableValue.getArtifactVariableType() + " is not supported for picking last deployed");
        }
      default:
        unhandled(triggerArtifactVariable.getType());
    }
    return null;
  }

  private String fetchLastCollectedArtifact(String artifactStreamId, String artifactFilter) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
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
      lastCollectedArtifact = artifactService.getArtifactByBuildNumber(artifactStream, artifactFilter, true);
      if (lastCollectedArtifact != null) {
        return lastCollectedArtifact.getUuid();
      } else {
        throw new WingsException("No artifact exist for trigger with artifact source " + artifactStream.getName());
      }
    }
  }

  private String getLastDeployedArtifacts(String appId, String workflowId, String variableName) {
    List<ArtifactVariable> lastDeployedArtifacts =
        workflowExecutionService.obtainLastGoodDeployedArtifactsVariables(appId, workflowId);

    Optional<ArtifactVariable> artifactVariable =
        lastDeployedArtifacts.stream()
            .filter(lastDeployedArtifact -> { return variableName.equals(lastDeployedArtifact.getName()); })
            .findFirst();

    if (artifactVariable.isPresent()) {
      return artifactVariable.get().getValue();
    } else {
      throw new WingsException("selected workflow does not have variable name " + variableName + " for trigger ");
    }
  }
}