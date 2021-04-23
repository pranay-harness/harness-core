package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class AddExplorerV2Indices extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_indices_explorerv2.sql";
  }
}
