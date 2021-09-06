package io.harness.migrations.timescaledb;

public class AddMaxStorageColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_max_storage_columns.sql";
  }
}
