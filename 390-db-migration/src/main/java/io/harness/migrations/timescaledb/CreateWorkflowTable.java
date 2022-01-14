package io.harness.migrations.timescaledb;

public class CreateWorkflowTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_workflow_table.sql";
  }
}
