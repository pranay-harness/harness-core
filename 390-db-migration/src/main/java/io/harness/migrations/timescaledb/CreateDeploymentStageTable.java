package io.harness.migrations.timescaledb;

public class CreateDeploymentStageTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_deployment_stage_table.sql";
  }
}
