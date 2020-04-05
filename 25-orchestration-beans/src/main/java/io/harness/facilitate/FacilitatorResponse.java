package io.harness.facilitate;

import io.harness.annotations.Redesign;
import io.harness.facilitate.modes.ExecutionMode;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
@Redesign
public class FacilitatorResponse {
  Duration initialWait;
  ExecutionMode executionMode;
  // Do a bunch of work u dont wanna repeat
  PassThroughData passThroughData;
}
