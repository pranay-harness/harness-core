package io.harness.migrations.timescaledb;

public class CreateServiceTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_service_table.sql";
  }
}
