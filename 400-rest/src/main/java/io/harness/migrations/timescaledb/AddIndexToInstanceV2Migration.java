package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._390_DB_MIGRATION)
public class AddIndexToInstanceV2Migration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_index_to_instance_v2_table.sql";
  }
}
