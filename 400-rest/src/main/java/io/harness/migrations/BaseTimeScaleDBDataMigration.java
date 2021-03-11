package io.harness.migrations;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class BaseTimeScaleDBDataMigration implements TimeScaleDBDataMigration {
  @Override
  public boolean migrate() {
    return true;
  }
}
