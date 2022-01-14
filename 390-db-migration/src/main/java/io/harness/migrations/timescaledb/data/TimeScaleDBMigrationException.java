package io.harness.migrations.timescaledb.data;

public class TimeScaleDBMigrationException extends RuntimeException {
  public TimeScaleDBMigrationException(Throwable cause) {
    super(cause);
  }
}
