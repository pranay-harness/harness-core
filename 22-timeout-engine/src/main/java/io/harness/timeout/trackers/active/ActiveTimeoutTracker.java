package io.harness.timeout.trackers.active;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutEvent;
import io.harness.timeout.trackers.PausableTimeoutTracker;

@OwnedBy(CDC)
public class ActiveTimeoutTracker extends PausableTimeoutTracker {
  public ActiveTimeoutTracker(long timeoutMillis, boolean running) {
    super(timeoutMillis, running);
  }

  @Override
  public Dimension getDimension() {
    return ActiveTimeoutTrackerFactory.DIMENSION;
  }

  @Override
  public void onEvent(TimeoutEvent event) {
    // TODO: If we get a non-active state then pause, else resume.
  }
}
