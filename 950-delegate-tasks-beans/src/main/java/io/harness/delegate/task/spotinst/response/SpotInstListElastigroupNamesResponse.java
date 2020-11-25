package io.harness.delegate.task.spotinst.response;

import io.harness.spotinst.model.ElastiGroup;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotInstListElastigroupNamesResponse implements SpotInstTaskResponse {
  private List<ElastiGroup> elastigroups;
}
