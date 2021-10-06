package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class StepData {
  String name;
  String type;
  boolean disabled;
}