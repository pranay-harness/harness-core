package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_ALB_SHIFT_SWAP_ROUTES;

import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotinstTrafficShiftAlbSwapRoutesParameters extends SpotInstTaskParameters {
  private ElastiGroup newElastigroup;
  private ElastiGroup oldElastigroup;
  private String elastigroupNamePrefix;
  private boolean downsizeOldElastigroup;
  private boolean rollback;
  private int newElastigroupWeight;
  private List<LbDetailsForAlbTrafficShift> details;

  @Builder
  public SpotinstTrafficShiftAlbSwapRoutesParameters(String appId, String accountId, String activityId,
      String commandName, String workflowExecutionId, Integer timeoutIntervalInMin, String awsRegion,
      ElastiGroup newElastigroup, ElastiGroup oldElastigroup, String elastigroupNamePrefix,
      boolean downsizeOldElastigroup, boolean rollback, List<LbDetailsForAlbTrafficShift> details,
      int newElastigroupWeight) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin,
        SPOT_INST_ALB_SHIFT_SWAP_ROUTES, awsRegion);
    this.newElastigroup = newElastigroup;
    this.oldElastigroup = oldElastigroup;
    this.elastigroupNamePrefix = elastigroupNamePrefix;
    this.downsizeOldElastigroup = downsizeOldElastigroup;
    this.rollback = rollback;
    this.details = details;
    this.newElastigroupWeight = newElastigroupWeight;
  }
}
