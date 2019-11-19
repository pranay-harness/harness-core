package migrations.timescaledb;

public class UpdateServiceGuardSchema extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/update_service_guard_config.sql";
  }
}
