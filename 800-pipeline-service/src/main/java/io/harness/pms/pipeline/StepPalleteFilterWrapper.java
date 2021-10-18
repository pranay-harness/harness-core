package io.harness.pms.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class StepPalleteFilterWrapper {
  List<StepPalleteModuleInfo> stepPalleteModuleInfos;
}
