package io.harness.timeout.trackers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerState;

@OwnedBy(CDC)
public abstract class PausableTimeoutTracker implements TimeoutTracker {
  private long timeoutMillis;
  private StopWatch stopWatch;

  public PausableTimeoutTracker(long timeoutMillis, boolean running) {
    this.timeoutMillis = timeoutMillis;
    this.stopWatch = new StopWatch(running);
  }

  protected boolean isTicking() {
    return stopWatch.isTicking();
  }

  protected void pause() {
    stopWatch.pause();
  }

  protected void resume() {
    stopWatch.resume();
  }

  @Override
  public Long getExpiryTime() {
    if (stopWatch.isTicking()) {
      return System.currentTimeMillis() + (timeoutMillis - stopWatch.getElapsedMillis());
    }
    return null;
  }

  @Override
  public TimeoutTrackerState getState() {
    if (stopWatch.getElapsedMillis() > timeoutMillis) {
      return TimeoutTrackerState.EXPIRED;
    }
    return stopWatch.isTicking() ? TimeoutTrackerState.TICKING : TimeoutTrackerState.PAUSED;
  }
}
