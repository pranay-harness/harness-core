package io.harness.migrations.timescaledb.data;

import io.harness.migrations.timescaledb.AbstractTimeScaleDBMigration;

public class CreateAnomaliesDataV2 extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_anomalies_v2_data_table.sql";
  }
}
