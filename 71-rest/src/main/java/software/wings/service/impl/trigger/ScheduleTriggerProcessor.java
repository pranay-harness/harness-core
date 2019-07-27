package software.wings.service.impl.trigger;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.scheduler.ScheduledTriggerJob;

@Singleton
@Slf4j
public class ScheduleTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    ScheduledCondition scheduledCondition = (ScheduledCondition) deploymentTrigger.getCondition();
    validateAndSetCronExpression(scheduledCondition);
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
  public void executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {}

  private void validateAndSetCronExpression(ScheduledCondition scheduledCondition) {
    try {
      if (isNotBlank(scheduledCondition.getCronExpression())) {
        ScheduledCondition scheduledConditionWithDesc =
            ScheduledCondition.builder()
                .cronExpression(scheduledCondition.getCronExpression())
                .cronDescription(triggerServiceHelper.getCronDescription(scheduledCondition.getCronExpression()))
                .onNewArtifactOnly(scheduledCondition.isOnNewArtifactOnly())
                .build();

        CronScheduleBuilder.cronSchedule(ScheduledTriggerJob.PREFIX + scheduledConditionWithDesc.getCronExpression());
      } else {
        throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Empty cron expression");
      }
    } catch (Exception ex) {
      logger.warn("Error parsing cron expression: {} : {}", scheduledCondition.getCronExpression(),
          ExceptionUtils.getMessage(ex));
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "Invalid cron expression");
    }
  }
}
