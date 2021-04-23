package io.harness.migrations;

import io.harness.annotations.dev.HarnessModule;
public class BaseMigration implements Migration, OnPrimaryManagerMigration {
  @Override
  public void migrate() {}
}
