package io.harness.migrations.timescaledb;

public class AddViewsSupportToAnomalies extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_views_support_to_anomalies.sql";
  }
}
