package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;

@OwnedBy(CDC)
@Redesign
public interface StepParameters {
  default Duration timeout() {
    return Duration.ofDays(10);
  }
}
