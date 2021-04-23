package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class CreateBudgetAlerts extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_budget_alerts_table.sql";
  }
}
