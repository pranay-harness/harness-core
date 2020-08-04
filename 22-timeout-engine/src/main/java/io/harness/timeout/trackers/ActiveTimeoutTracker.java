package io.harness.timeout.trackers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutEventParameters;

@OwnedBy(CDC)
public class ActiveTimeoutTracker extends PausableTimeoutTracker {
  public ActiveTimeoutTracker(long timeoutMillis, boolean running) {
    super(timeoutMillis, running);
  }

  @Override
  public String dimension() {
    return "ACTIVE";
  }

  @Override
  public void onEvent(String eventType, TimeoutEventParameters eventParameters) {
    // TODO: If we get a non-active state then pause, else resume.
  }
}
