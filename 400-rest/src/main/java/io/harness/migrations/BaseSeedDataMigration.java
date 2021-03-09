package io.harness.migrations;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._390_DB_MIGRATION)
public class BaseSeedDataMigration implements SeedDataMigration {
  @Override
  public void migrate() {}
}
