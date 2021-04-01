package io.harness.migrations;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class BaseMigration implements Migration, OnPrimaryManagerMigration {
  @Override
  public void migrate() {}
}
