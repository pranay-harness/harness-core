package io.harness.timeout.trackers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerState;

@OwnedBy(CDC)
public class AbsoluteTimeoutTracker implements TimeoutTracker {
  private final long timeoutMillis;
  private final long startTimeMillis;

  public AbsoluteTimeoutTracker(long timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
    this.startTimeMillis = System.currentTimeMillis();
  }

  @Override
  public String dimension() {
    return "ABSOLUTE";
  }

  @Override
  public Long getExpiryTime() {
    return startTimeMillis + timeoutMillis;
  }

  @Override
  public TimeoutTrackerState getState() {
    return System.currentTimeMillis() > startTimeMillis + timeoutMillis ? TimeoutTrackerState.EXPIRED
                                                                        : TimeoutTrackerState.TICKING;
  }
}
