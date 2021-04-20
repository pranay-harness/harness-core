package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class UpdateServiceGuardSchema extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/update_service_guard_config.sql";
  }
}
