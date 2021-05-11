package io.harness.migrations.timescaledb.data;

import io.harness.annotations.dev.HarnessModule;
public class TimeScaleDBMigrationException extends RuntimeException {
  public TimeScaleDBMigrationException(Throwable cause) {
    super(cause);
  }
}
