package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class RenameInstanceMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/rename_instance_table.sql";
  }
}
