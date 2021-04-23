package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class UniqueIndexCEUtilizationDataTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/unique_index_ce_utilization_data_tables.sql";
  }
}
