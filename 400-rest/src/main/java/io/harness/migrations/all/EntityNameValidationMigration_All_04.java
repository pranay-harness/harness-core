package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._390_DB_MIGRATION)
public class EntityNameValidationMigration_All_04 extends EntityNameValidationMigration {
  @Override
  protected boolean skipAccount(String accountId) {
    return false;
  }
}
