package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class UpdateServiceGuardSchema extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/update_service_guard_config.sql";
  }
}
