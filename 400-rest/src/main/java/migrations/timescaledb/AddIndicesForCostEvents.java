package migrations.timescaledb;

public class AddIndicesForCostEvents extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_indices_cost_events.sql";
  }
}
