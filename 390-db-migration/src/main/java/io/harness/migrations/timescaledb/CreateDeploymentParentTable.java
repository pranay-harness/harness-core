package io.harness.migrations.timescaledb;

public class CreateDeploymentParentTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_deployment_parent_table.sql";
  }
}
