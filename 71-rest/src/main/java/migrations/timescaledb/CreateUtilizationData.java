package migrations.timescaledb;

public class CreateUtilizationData extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_utilization_data_table.sql";
  }
}
