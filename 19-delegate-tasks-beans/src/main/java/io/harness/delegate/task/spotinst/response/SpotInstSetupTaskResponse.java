package io.harness.delegate.task.spotinst.response;

import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpotInstSetupTaskResponse implements SpotInstTaskResponse {
  private ElastiGroup newElastiGroup;
  // Will be used during rollback, to restore this group to previous capacity
  private List<ElastiGroup> groupToBeDownsized;

  private String prodListenerArn;
  private String stageListenerArn;
}