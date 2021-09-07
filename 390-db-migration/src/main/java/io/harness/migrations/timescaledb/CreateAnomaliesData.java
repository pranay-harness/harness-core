package io.harness.migrations.timescaledb;

public class CreateAnomaliesData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_anomalies_data_table.sql";
  }
}
