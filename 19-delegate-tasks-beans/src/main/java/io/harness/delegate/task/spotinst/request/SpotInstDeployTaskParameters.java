package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_DEPLOY;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstDeployTaskParameters extends SpotInstTaskParameters {
  private int newElastiGroupDesiredInstances;

  public SpotInstDeployTaskParameters(String accountId, String appId, String commandName, String activityId,
      Integer timeoutIntervalInMin, int newElastiGroupDesiredInstances, String workflowExecutionId, String awsRegion) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin, SPOT_INST_DEPLOY,
        awsRegion);
    this.newElastiGroupDesiredInstances = newElastiGroupDesiredInstances;
  }
}