package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class InitVerificationSchemaMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_verification.sql";
  }
}
