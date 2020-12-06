package migrations.timescaledb;

public class AddCostEvents extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_cost_events.sql";
  }
}
