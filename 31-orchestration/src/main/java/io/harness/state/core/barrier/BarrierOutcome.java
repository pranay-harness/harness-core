package io.harness.state.core.barrier;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class BarrierOutcome implements Outcome {
  String message;
  String identifier;
}
