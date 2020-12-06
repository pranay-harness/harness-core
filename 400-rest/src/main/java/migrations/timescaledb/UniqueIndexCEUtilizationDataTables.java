package migrations.timescaledb;

public class UniqueIndexCEUtilizationDataTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/unique_index_ce_utilization_data_tables.sql";
  }
}
