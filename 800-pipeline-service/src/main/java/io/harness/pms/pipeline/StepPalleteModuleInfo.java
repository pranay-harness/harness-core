package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@Schema(name = "StepPalleteModuleInfo", description = "This is the view of the Step Pallete")
public class StepPalleteModuleInfo {
  String module;
  String category;
  boolean shouldShowCommonSteps;
  String commonStepCategory;
}
