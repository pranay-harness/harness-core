package io.harness.migrations.timescaledb;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CE)
public class CreateCDCServiceTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_change_data_capture_tables.sql";
  }
}
