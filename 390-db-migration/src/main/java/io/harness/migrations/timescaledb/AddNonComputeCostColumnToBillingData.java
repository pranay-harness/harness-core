package io.harness.migrations.timescaledb;

public class AddNonComputeCostColumnToBillingData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_non_compute_cost_billing_data_table.sql";
  }
}
