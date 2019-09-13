package software.wings.service.impl.trigger;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.Action.ActionType;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Singleton
@Slf4j
public class ScheduleTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;
  @Inject private transient TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject private transient TriggerDeploymentExecution triggerDeploymentExecution;
  @Inject private transient ExecutorService executorService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient ScheduleTriggerHandler scheduleTriggerHandler;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    ScheduledCondition scheduledCondition = (ScheduledCondition) deploymentTrigger.getCondition();
    validateAndHandleCronExpression(deploymentTrigger, existingTrigger, scheduledCondition);
    updateCronScheduler();
  }

  @Override
  public void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    triggerServiceHelper.validateTriggerAction(deploymentTrigger);
  }

  @Override
  public void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger) {
    // No need to update anything for schedule trigger
  }

  @Override
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger);
  }

  @Override
  public WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {
    executorService.execute(() -> {
      ScheduledTriggerExecutionParams scheduledTriggerExecutionParams =
          (ScheduledTriggerExecutionParams) triggerExecutionParams;

      DeploymentTrigger trigger = scheduledTriggerExecutionParams.getTrigger();
      if (trigger.isTriggerDisabled()) {
        logger.warn("Trigger is disabled for appId {}, Trigger Id {} and name {} with the scheduled fire time ",
            trigger.getAppId(), trigger.getUuid(), trigger.getName());
        return;
      }
      logger.info("Received scheduled trigger for appId {}, Trigger Id {} and name {} with the scheduled fire time ",
          trigger.getAppId(), trigger.getUuid(), trigger.getName());

      ScheduledCondition scheduledCondition = (ScheduledCondition) (trigger.getCondition());
      if (scheduledCondition.isOnNewArtifactOnly()) {
        if (trigger.getAction() != null && trigger.getAction().getActionType().equals(ActionType.WORKFLOW)) {
          List<ArtifactVariable> artifactVariables =
              triggerArtifactVariableHandler.fetchArtifactVariablesForExecution(trigger.getAppId(), trigger, null);

          WorkflowAction workflowAction = (WorkflowAction) trigger.getAction();

          List<ArtifactVariable> lastDeployedArtifactVariables =
              workflowExecutionService.obtainLastGoodDeployedArtifactsVariables(appId, workflowAction.getWorkflowId());

          if (lastDeployedArtifactVariables.stream()
                  .map(artifactVariable -> artifactVariable.getValue())
                  .collect(toList())
                  .containsAll(artifactVariables.stream()
                                   .map(artifactVariable -> artifactVariable.getValue())
                                   .collect(toList()))) {
            logger.info("No new version of artifacts found from the last successful execution "
                    + "of pipeline/ workflow {}. So, not triggering execution",
                workflowAction.getWorkflowId());
            return;
          } else {
            logger.info("New version of artifacts found from the last successful execution "
                    + "of pipeline/ workflow {}. So, triggering  execution",
                workflowAction.getWorkflowId());
          }
        }
      }

      triggerDeploymentExecution.executeDeployment(trigger,
          triggerArtifactVariableHandler.fetchArtifactVariablesForExecution(trigger.getAppId(), trigger, null));
    });
    return null;
  }

  private void validateAndHandleCronExpression(
      DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger, ScheduledCondition scheduledCondition) {
    try {
      String cronExpression = scheduledCondition.getCronExpression();
      if (cronExpression == null) {
        throw new TriggerException("cronExpression is null for trigger " + deploymentTrigger.getName(), null);
      }

      if (isNotBlank(scheduledCondition.getCronExpression())) {
        ScheduledCondition scheduledConditionWithDesc =
            ScheduledCondition.builder()
                .cronExpression(cronExpression)
                .cronDescription(triggerServiceHelper.getCronDescription(cronExpression))
                .onNewArtifactOnly(scheduledCondition.isOnNewArtifactOnly())
                .build();

        deploymentTrigger.setCondition(scheduledConditionWithDesc);
        deploymentTrigger.setNextIterations(null);
      } else {
        throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Empty cron expression");
      }
    } catch (Exception ex) {
      logger.warn("Error parsing cron expression: {} : {}", scheduledCondition.getCronExpression(),
          ExceptionUtils.getMessage(ex));
      throw new TriggerException(ex.getMessage(), null);
    }
  }

  private void updateCronScheduler() {
    scheduleTriggerHandler.wakeup();
  }

  @Value
  @Builder
  public static class ScheduledTriggerExecutionParams implements TriggerExecutionParams {
    DeploymentTrigger trigger;
  }
}
