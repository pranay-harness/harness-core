package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class AddIndexToInstanceV2Migration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_index_to_instance_v2_table.sql";
  }
}
