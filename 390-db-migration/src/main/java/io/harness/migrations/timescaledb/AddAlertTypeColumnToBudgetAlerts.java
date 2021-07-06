package io.harness.migrations.timescaledb;

public class AddAlertTypeColumnToBudgetAlerts extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_alert_type_column_budget_alerts_table.sql";
  }
}
