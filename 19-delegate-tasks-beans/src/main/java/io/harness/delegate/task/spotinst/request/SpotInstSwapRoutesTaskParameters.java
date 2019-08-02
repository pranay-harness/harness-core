package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_SWAP_ROUTES;

import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstSwapRoutesTaskParameters extends SpotInstTaskParameters {
  private ElastiGroup newElastiGroup;
  private ElastiGroup oldElastiGroup;
  private String prodListenerArn;
  private String stageListenerArn;
  private String elastiGroupNamePrefix;
  private String targetGroupArnForNewElastiGroup;
  private String targetGroupArnForOldElastiGroup;
  private boolean downsizeOldElastiGroup;
  private boolean rollback;
  private int steadyStateTimeOut;

  @Builder
  public SpotInstSwapRoutesTaskParameters(String appId, String accountId, String activityId, String commandName,
      String workflowExecutionId, Integer timeoutIntervalInMin, String awsRegion, ElastiGroup newElastiGroup,
      ElastiGroup oldElastiGroup, String prodListenerArn, String stageListenerArn, boolean downsizeOldElastiGroup,
      boolean rollback, String elastiGroupNamePrefix, String targetGroupArnForNewElastiGroup,
      String targetGroupArnForOldElastiGroup, int steadyStateTimeOut) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin, SPOT_INST_SWAP_ROUTES,
        awsRegion);
    this.newElastiGroup = newElastiGroup;
    this.oldElastiGroup = oldElastiGroup;
    this.prodListenerArn = prodListenerArn;
    this.stageListenerArn = stageListenerArn;
    this.downsizeOldElastiGroup = downsizeOldElastiGroup;
    this.rollback = rollback;
    this.elastiGroupNamePrefix = elastiGroupNamePrefix;
    this.targetGroupArnForNewElastiGroup = targetGroupArnForNewElastiGroup;
    this.targetGroupArnForOldElastiGroup = targetGroupArnForOldElastiGroup;
    this.steadyStateTimeOut = steadyStateTimeOut;
  }
}