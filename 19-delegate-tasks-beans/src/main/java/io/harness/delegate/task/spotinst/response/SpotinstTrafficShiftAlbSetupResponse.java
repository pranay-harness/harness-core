package io.harness.delegate.task.spotinst.response;

import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpotinstTrafficShiftAlbSetupResponse implements SpotInstTaskResponse {
  private ElastiGroup newElastigroup;
  private List<ElastiGroup> elastiGroupsToBeDownsized;
  private List<LbDetailsForAlbTrafficShift> lbDetailsWithTargetGroups;
}