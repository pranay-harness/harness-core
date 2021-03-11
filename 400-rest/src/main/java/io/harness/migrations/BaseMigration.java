package io.harness.migrations;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class BaseMigration implements Migration, OnPrimaryManagerMigration {
  @Override
  public void migrate() {}
}
