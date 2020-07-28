package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@OwnedBy(CDC)
@Value
@Builder
public class BarrierStepParameters implements StepParameters {
  String identifier;
  long timeoutInMillis;

  @Override
  public Duration timeout() {
    return Duration.ofMillis(timeoutInMillis);
  }
}
