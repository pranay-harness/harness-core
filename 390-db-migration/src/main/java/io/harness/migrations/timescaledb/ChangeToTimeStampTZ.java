package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class ChangeToTimeStampTZ extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/change_timestamp.sql";
  }
}
