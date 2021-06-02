package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

public class EntityNameValidationMigration_All_04 extends EntityNameValidationMigration {
  @Override
  protected boolean skipAccount(String accountId) {
    return false;
  }
}
