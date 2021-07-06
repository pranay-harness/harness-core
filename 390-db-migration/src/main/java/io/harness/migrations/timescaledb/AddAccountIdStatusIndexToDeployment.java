package io.harness.migrations.timescaledb;

public class AddAccountIdStatusIndexToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_accountId_status_index_to_deployment.sql";
  }
}
