package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface TimeoutTracker {
  String dimension();
  Long getExpiryTime();
  TimeoutTrackerState getState();

  default void onEvent(String eventType, TimeoutEventParameters eventParameters) {
    // Ignore all events by default.
  }
}
