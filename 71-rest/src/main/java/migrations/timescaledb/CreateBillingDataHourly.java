package migrations.timescaledb;

public class CreateBillingDataHourly extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_billing_data_hourly_table.sql";
  }
}
