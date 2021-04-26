package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class InitVerificationSchemaMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_verification.sql";
  }
}
