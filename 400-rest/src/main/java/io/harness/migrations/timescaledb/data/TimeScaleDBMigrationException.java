package io.harness.migrations.timescaledb.data;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class TimeScaleDBMigrationException extends RuntimeException {
  public TimeScaleDBMigrationException(Throwable cause) {
    super(cause);
  }
}
