package migrations.timescaledb;

public class CreateBillingData extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_billing_data_table.sql";
  }
}
