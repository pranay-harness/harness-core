package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class ServicePipelineInfo {
  String pipelineExecutionId;
  String identifier;
  String name;
  String status;
  long lastExecutedAt;
}
