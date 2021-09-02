package io.harness.migrations.timescaledb;

public class AddProjectSummaryStatDataTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_project_summary_data_tables.sql";
  }
}
