package io.harness.migrations;

import io.harness.annotations.dev.HarnessModule;
public class BaseTimeScaleDBDataMigration implements TimeScaleDBDataMigration {
  @Override
  public boolean migrate() {
    return true;
  }
}
