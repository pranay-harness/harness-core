/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.timeseries;

import io.harness.logging.AutoLogContext;

import java.util.UUID;

public class TimeseriesLogContext extends AutoLogContext {
  private static final String ID_KEY = "timeseries_id";

  public TimeseriesLogContext(OverrideBehavior behavior) {
    super(ID_KEY, UUID.randomUUID().toString(), behavior);
  }
}
