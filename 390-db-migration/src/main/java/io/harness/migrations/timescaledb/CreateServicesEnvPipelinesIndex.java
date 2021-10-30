package io.harness.migrations.timescaledb;

public class CreateServicesEnvPipelinesIndex extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_services_env_pipelines_index.sql";
  }
}
