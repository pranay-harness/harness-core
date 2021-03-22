package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class UniqueIndexCEUtilizationDataTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/unique_index_ce_utilization_data_tables.sql";
  }
}
