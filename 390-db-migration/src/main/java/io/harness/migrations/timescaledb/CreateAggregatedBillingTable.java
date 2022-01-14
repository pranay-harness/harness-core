package io.harness.migrations.timescaledb;

public class CreateAggregatedBillingTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_billing_data_aggregated_table.sql";
  }
}
