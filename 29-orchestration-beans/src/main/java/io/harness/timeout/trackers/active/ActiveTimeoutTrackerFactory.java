package io.harness.timeout.trackers.active;

import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;

public class ActiveTimeoutTrackerFactory implements TimeoutTrackerFactory<ActiveTimeoutParameters> {
  public static final Dimension DIMENSION = Dimension.builder().type(Dimension.ACTIVE).build();

  @Override
  public TimeoutTracker create(ActiveTimeoutParameters parameters) {
    return new ActiveTimeoutTracker(parameters.getTimeoutMillis(), parameters.isRunningAtStart());
  }
}
