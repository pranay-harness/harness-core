package io.harness.event.timeseries;

import io.harness.logging.AutoLogContext;

import java.util.UUID;

public class TimeseriesLogContext extends AutoLogContext {
  private static final String ID_KEY = "timeseries_id";

  public TimeseriesLogContext(OverrideBehavior behavior) {
    super(ID_KEY, UUID.randomUUID().toString(), behavior);
  }
}
