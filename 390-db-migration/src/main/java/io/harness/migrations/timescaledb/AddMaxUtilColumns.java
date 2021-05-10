package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class AddMaxUtilColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_maxutil_column_k8s_util_table.sql";
  }
}
