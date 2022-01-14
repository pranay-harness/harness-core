package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@Schema(name = "StepPalleteModuleInfo", description = "This has details of the Step Palette.")
public class StepPalleteModuleInfo {
  @Schema(description = "Module Type like CD/CI etc") String module;
  @Schema(description = "Step Category like Approval/Provisioner etc") String category;
  @Schema(description = "Whether Pallete should list the common Steps") boolean shouldShowCommonSteps;
  @Schema(description = "Category for common Steps") String commonStepCategory;
}
