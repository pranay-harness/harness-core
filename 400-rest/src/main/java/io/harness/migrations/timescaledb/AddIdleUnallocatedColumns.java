package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._390_DB_MIGRATION)
public class AddIdleUnallocatedColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_idle_unallocated_columns.sql";
  }
}
