package migrations.timescaledb;

public class CreateBillingData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_billing_data_table.sql";
  }
}
