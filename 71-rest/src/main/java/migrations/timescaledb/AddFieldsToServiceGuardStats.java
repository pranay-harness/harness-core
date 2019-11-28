package migrations.timescaledb;

public class AddFieldsToServiceGuardStats extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_fields_to_service_guard_stats.sql";
  }
}
