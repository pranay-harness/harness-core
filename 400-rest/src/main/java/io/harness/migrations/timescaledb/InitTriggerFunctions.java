package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._390_DB_MIGRATION)
public class InitTriggerFunctions extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/trigger_functions.sql";
  }
}
