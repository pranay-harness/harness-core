package io.harness.migrations.timescaledb;

public class InitSchemaMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_script.sql";
  }
}
