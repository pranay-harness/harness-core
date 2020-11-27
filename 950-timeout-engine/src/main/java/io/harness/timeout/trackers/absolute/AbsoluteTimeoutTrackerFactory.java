package io.harness.timeout.trackers.absolute;

import io.harness.timeout.Dimension;
import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;

public class AbsoluteTimeoutTrackerFactory implements TimeoutTrackerFactory<AbsoluteTimeoutParameters> {
  public static final Dimension DIMENSION = Dimension.builder().type(Dimension.ABSOLUTE).build();

  @Override
  public TimeoutTracker create(AbsoluteTimeoutParameters parameters) {
    return new AbsoluteTimeoutTracker(parameters.getTimeoutMillis());
  }
}
