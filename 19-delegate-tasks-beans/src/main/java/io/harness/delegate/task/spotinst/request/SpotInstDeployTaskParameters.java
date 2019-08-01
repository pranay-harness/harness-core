package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_DEPLOY;

import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstDeployTaskParameters extends SpotInstTaskParameters {
  private ElastiGroup newElastiGroupWithUpdatedCapacity;
  private ElastiGroup oldElastiGroupWithUpdatedCapacity;

  @Builder
  public SpotInstDeployTaskParameters(String accountId, String appId, String commandName, String activityId,
      Integer timeoutIntervalInMin, ElastiGroup newElastiGroupWithUpdatedCapacity,
      ElastiGroup oldElastiGroupWithUpdatedCapacity, String workflowExecutionId, String awsRegion) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin, SPOT_INST_DEPLOY,
        awsRegion);
    this.newElastiGroupWithUpdatedCapacity = newElastiGroupWithUpdatedCapacity;
    this.oldElastiGroupWithUpdatedCapacity = oldElastiGroupWithUpdatedCapacity;
  }
}