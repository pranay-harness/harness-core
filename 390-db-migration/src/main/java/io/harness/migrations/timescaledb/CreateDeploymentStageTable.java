package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class CreateDeploymentStageTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_deployment_stage_table.sql";
  }
}
