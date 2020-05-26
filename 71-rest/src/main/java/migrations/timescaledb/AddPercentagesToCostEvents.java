package migrations.timescaledb;

public class AddPercentagesToCostEvents extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_cost_event_percentages.sql";
  }
}
