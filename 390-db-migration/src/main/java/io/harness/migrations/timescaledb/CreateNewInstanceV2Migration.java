package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class CreateNewInstanceV2Migration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_instance_v2_table.sql";
  }
}
