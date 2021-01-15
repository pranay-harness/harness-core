package io.harness.timeout.trackers.absolute;

import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.contracts.Dimension;

public class AbsoluteTimeoutTrackerFactory implements TimeoutTrackerFactory<AbsoluteTimeoutParameters> {
  public static final Dimension DIMENSION = Dimension.newBuilder().setType("ABSOLUTE").build();

  @Override
  public TimeoutTracker create(AbsoluteTimeoutParameters parameters) {
    return new AbsoluteTimeoutTracker(parameters.getTimeoutMillis());
  }
}
