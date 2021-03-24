package io.harness.migrations;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._390_DB_MIGRATION)
public interface OnPrimaryManagerMigration {
  void migrate();
}
