package io.harness.pms.sdk.core.supporter.child;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class TestSectionSweepingOutput implements ExecutionSweepingOutput {
  Map<String, String> map;
}
