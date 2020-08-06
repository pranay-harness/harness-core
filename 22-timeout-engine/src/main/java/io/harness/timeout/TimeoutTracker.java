package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface TimeoutTracker {
  Dimension getDimension();
  Long getExpiryTime();
  TimeoutTrackerState getState();

  default void onEvent(TimeoutEvent event) {
    // Ignore all events by default.
  }
}
