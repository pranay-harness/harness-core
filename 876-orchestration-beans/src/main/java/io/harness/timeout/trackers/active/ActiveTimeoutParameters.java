package io.harness.timeout.trackers.active;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutParameters;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ActiveTimeoutParameters implements TimeoutParameters {
  long timeoutMillis;
  boolean runningAtStart;
}
