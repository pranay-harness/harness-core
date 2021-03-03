package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class CreateInstanceStatsHourTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_instance_stats_hour_table.sql";
  }
}
