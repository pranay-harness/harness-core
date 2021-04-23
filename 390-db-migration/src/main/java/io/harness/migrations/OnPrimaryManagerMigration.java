package io.harness.migrations;

import io.harness.annotations.dev.HarnessModule;
public interface OnPrimaryManagerMigration {
  void migrate();
}
