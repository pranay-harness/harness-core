package io.harness.migrations.timescaledb;

public class CreateInstanceStatsHourTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_instance_stats_hour_table.sql";
  }
}
