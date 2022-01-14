package io.harness.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class SectionStepSweepingOutput implements ExecutionSweepingOutput {
  @Singular List<String> failedNodeIds;
}
