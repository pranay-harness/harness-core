package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class AddSystemCostBillingData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_system_cost_billing_data_table.sql";
  }
}
