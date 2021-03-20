package io.harness.migrations.timescaledb.data;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class TimeScaleDBMigrationException extends RuntimeException {
  public TimeScaleDBMigrationException(Throwable cause) {
    super(cause);
  }
}
