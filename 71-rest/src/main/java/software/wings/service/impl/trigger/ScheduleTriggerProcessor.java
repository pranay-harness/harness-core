package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
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
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger, boolean readPrimaryVariablesValueNames) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger, readPrimaryVariablesValueNames);
  }

  @Override
  public WorkflowExecution executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {
    executorService.execute(() -> {
      ScheduledTriggerExecutionParams scheduledTriggerExecutionParams =
          (ScheduledTriggerExecutionParams) triggerExecutionParams;

      DeploymentTrigger trigger = scheduledTriggerExecutionParams.getTrigger();

      logger.info("Received scheduled trigger for appId {}, Trigger Id {} and name {} with the scheduled fire time ",
          trigger.getAppId(), trigger.getUuid(), trigger.getName());

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
