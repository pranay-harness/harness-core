/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout.trackers.active;

import io.harness.timeout.TimeoutTracker;
import io.harness.timeout.TimeoutTrackerFactory;
import io.harness.timeout.contracts.Dimension;

public class ActiveTimeoutTrackerFactory implements TimeoutTrackerFactory<ActiveTimeoutParameters> {
  public static final Dimension DIMENSION = Dimension.newBuilder().setType("ACTIVE").build();

  @Override
  public TimeoutTracker create(ActiveTimeoutParameters parameters) {
    return new ActiveTimeoutTracker(parameters.getTimeoutMillis(), parameters.isRunningAtStart());
  }
}
