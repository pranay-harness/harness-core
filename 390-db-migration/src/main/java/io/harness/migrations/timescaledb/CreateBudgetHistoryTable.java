package io.harness.migrations.timescaledb;

public class CreateBudgetHistoryTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_budget_history_table.sql";
  }
}
