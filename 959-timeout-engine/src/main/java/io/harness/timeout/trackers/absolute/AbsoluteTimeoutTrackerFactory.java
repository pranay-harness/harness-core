/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
