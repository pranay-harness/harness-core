package io.harness.migrations.timescaledb.data;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.timescaledb.AbstractTimeScaleDBMigration;

@TargetModule(Module._390_DB_MIGRATION)
public class CreateAnomaliesDataV2 extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_anomalies_v2_data_table.sql";
  }
}
