package io.harness.delegate.task.spotinst.response;

import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.spotinst.model.ElastiGroup;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotInstSetupTaskResponse implements SpotInstTaskResponse {
  private ElastiGroup newElastiGroup;
  // Will be used during rollback, to restore this group to previous capacity
  private List<ElastiGroup> groupToBeDownsized;
  private List<LoadBalancerDetailsForBGDeployment> lbDetailsForBGDeployments;
}
