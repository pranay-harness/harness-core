package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class DeploymentAdditionalColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/deployment_table_add_cols.sql";
  }
}
