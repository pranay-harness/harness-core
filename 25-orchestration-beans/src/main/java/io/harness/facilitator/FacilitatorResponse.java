package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.ExecutionMode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class FacilitatorResponse {
  @Builder.Default Duration initialWait = Duration.ofSeconds(0);
  @NonNull ExecutionMode executionMode;
  // This is for the micro level optimization during no mode evaluation you might do a bunch of work which you don't
  // want to repeat you can use this object to pass that data through
  PassThroughData passThroughData;
}
