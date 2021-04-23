package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class CreateAggregatedBillingTable extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_billing_data_aggregated_table.sql";
  }
}
