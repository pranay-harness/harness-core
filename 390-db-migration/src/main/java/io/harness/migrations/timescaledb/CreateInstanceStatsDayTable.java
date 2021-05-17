package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class CreateInstanceStatsDayTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_instance_stats_day_table.sql";
  }
}
