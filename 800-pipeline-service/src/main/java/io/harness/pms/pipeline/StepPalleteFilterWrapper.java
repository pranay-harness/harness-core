package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@Schema(name = "StepPalleteFilterWrapper", description = "This has details of the Step Palette Filter.")
public class StepPalleteFilterWrapper {
  @Schema(description = "List of Step Pallete Module Info") List<StepPalleteModuleInfo> stepPalleteModuleInfos;
}
