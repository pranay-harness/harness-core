package migrations.timescaledb;

public class AddSystemCostBillingData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_system_cost_billing_data_table.sql";
  }
}
