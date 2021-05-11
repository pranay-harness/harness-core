package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class AddIdleUnallocatedColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_idle_unallocated_columns.sql";
  }
}
