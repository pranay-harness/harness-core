package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class CreateUtilizationData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_utilization_data_table.sql";
  }
}
