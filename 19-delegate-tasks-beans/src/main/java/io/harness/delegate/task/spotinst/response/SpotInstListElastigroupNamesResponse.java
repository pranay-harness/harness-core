package io.harness.delegate.task.spotinst.response;

import io.harness.spotinst.model.ElastiGroup;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpotInstListElastigroupNamesResponse implements SpotInstTaskResponse {
  private List<ElastiGroup> elastigroups;
}
