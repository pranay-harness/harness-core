package migrations.timescaledb;

public class AlterCEUtilizationDataTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/alter_ce_utilization_data_tables.sql";
  }
}
