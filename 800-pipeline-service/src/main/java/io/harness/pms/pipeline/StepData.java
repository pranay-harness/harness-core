package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.constants.FeatureRestrictionName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@Schema(name = "StepData", description = "This contains metadata about step.")
public class StepData {
  String name;
  String type;
  boolean disabled;
  FeatureRestrictionName featureRestrictionName;
}