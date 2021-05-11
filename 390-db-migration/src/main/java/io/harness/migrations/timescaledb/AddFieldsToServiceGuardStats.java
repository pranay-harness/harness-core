package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class AddFieldsToServiceGuardStats extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_fields_to_service_guard_stats.sql";
  }
}
