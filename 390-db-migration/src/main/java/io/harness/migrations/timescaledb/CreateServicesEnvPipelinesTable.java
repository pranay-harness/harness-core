package io.harness.migrations.timescaledb;

public class CreateServicesEnvPipelinesTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_services_env_pipelines.sql";
  }
}
