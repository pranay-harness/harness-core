package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class AddSchemaForServiceGuardStats extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_schema_service_guard.sql";
  }
}
