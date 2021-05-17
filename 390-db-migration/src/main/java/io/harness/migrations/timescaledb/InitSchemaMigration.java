package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class InitSchemaMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_script.sql";
  }
}
