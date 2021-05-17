package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class AddCostEvents extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_cost_events.sql";
  }
}
