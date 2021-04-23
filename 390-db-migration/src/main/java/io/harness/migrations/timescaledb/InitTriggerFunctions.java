package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class InitTriggerFunctions extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/trigger_functions.sql";
  }
}
