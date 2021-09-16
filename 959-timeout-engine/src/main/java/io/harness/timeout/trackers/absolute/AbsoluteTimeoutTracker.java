package io.harness.timeout.trackers.absolute;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerState;
import io.harness.timeout.contracts.Dimension;

import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@TypeAlias("absoluteTimeoutTracker")
public class AbsoluteTimeoutTracker implements TimeoutTracker {
  private long timeoutMillis;
  private long startTimeMillis;

  public AbsoluteTimeoutTracker(long timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
    this.startTimeMillis = System.currentTimeMillis();
  }

  @Override
  public Dimension getDimension() {
    return AbsoluteTimeoutTrackerFactory.DIMENSION;
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
