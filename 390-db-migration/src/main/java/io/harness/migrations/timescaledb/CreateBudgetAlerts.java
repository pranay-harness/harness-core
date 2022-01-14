package io.harness.migrations.timescaledb;

public class CreateBudgetAlerts extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_budget_alerts_table.sql";
  }
}
