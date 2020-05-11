package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.trigger.Action.ActionType.PIPELINE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.PipelineCondition;
import software.wings.service.intfc.PipelineService;

import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class PipelineTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient PipelineService pipelineService;
  @Inject private transient TriggerDeploymentExecution triggerDeploymentExecution;
  @Inject private transient ExecutorService executorService;
  @Inject private transient TriggerArtifactVariableHandler triggerArtifactVariableHandler;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    PipelineCondition pipelineCondition = (PipelineCondition) deploymentTrigger.getCondition();
    validatePipelineId(deploymentTrigger, pipelineCondition);
  }

  @Override
  public void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    triggerServiceHelper.validateTriggerAction(deploymentTrigger);
  }

  @Override
  public void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger) {
    PipelineCondition pipelineCondition = (PipelineCondition) deploymentTrigger.getCondition();

    try {
      String pipelineName =
          pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineCondition.getPipelineId());
      deploymentTrigger.setCondition(
          PipelineCondition.builder().pipelineId(pipelineCondition.getPipelineId()).pipelineName(pipelineName).build());
    } catch (WingsException exception) {
      throw new TriggerException(
          "Pipeline does not exist for pipeline name " + pipelineCondition.getPipelineName(), USER);
    }
  }

  @Override
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger, boolean readPrimaryVariablesValueNames) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger, readPrimaryVariablesValueNames);
  }

  @Override
  public WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {
    PipelineTriggerExecutionParams pipelineTriggerExecutionParams =
        (PipelineTriggerExecutionParams) triggerExecutionParams;

    logger.info(
        "Received Pipeline completion request for pipeline id {}", pipelineTriggerExecutionParams.getPipelineId());

    executorService.execute(() -> {
      triggerServiceHelper.getTriggersMatchesWorkflow(appId, pipelineTriggerExecutionParams.getPipelineId())
          .forEach(trigger -> {
            if (trigger.isTriggerDisabled()) {
              logger.warn("Trigger is disabled for appId {}, Trigger Id {} and name {}for pipeline id  {} ",
                  trigger.getAppId(), trigger.getUuid(), pipelineTriggerExecutionParams.getPipelineId());
              return;
            }
            triggerDeploymentExecution.executeDeployment(trigger,
                triggerArtifactVariableHandler.fetchArtifactVariablesForExecution(trigger.getAppId(), trigger, null));
          });
    });

    return null;
  }

  private void validatePipelineId(DeploymentTrigger deploymentTrigger, PipelineCondition pipelineCondition) {
    String pipelineName;
    try {
      if (pipelineCondition.getPipelineId() == null) {
        throw new TriggerException("pipeline Id is null for trigger " + deploymentTrigger.getName(), null);
      }

      pipelineName = pipelineService.fetchPipelineName(deploymentTrigger.getAppId(), pipelineCondition.getPipelineId());
    } catch (WingsException exception) {
      logger.warn("Pipeline does not exist for pipeline id {} triggerId { } appId {}",
          pipelineCondition.getPipelineId(), deploymentTrigger.getUuid(), deploymentTrigger.getAppId(), USER);
      throw new TriggerException(
          "Pipeline does not exist for pipeline name " + pipelineCondition.getPipelineName(), USER);
    }
    if (deploymentTrigger.getAction().getActionType() == PIPELINE
        && ((PipelineCondition) deploymentTrigger.getCondition())
               .getPipelineId()
               .equals(((PipelineAction) deploymentTrigger.getAction()).getPipelineId())) {
      logger.warn("Trigger condition pipeline " + pipelineName + " is same as that of action {} triggerId { } appId {}",
          pipelineCondition.getPipelineId(), deploymentTrigger.getUuid(), deploymentTrigger.getAppId(), USER);
      throw new TriggerException("Trigger condition pipeline " + pipelineName + " is same as that of action ", USER);
    }
  }

  @Value
  @Builder
  public static class PipelineTriggerExecutionParams implements TriggerExecutionParams {
    String pipelineId;
  }
}
