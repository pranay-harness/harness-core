package io.harness.migrations.timescaledb;

public class InitTriggerFunctions extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/trigger_functions.sql";
  }
}
