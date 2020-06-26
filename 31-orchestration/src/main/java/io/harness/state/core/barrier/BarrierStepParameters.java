package io.harness.state.core.barrier;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class BarrierStepParameters implements StepParameters {
  String identifier;
  long timeoutInMillis;
}
