package migrations.timescaledb;

public class AddRequestColumnToBillingData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_request_limit_billing_data_table.sql";
  }
}
