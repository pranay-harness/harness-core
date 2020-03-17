package migrations.timescaledb;

public class UpdateServiceGuardSchema extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/update_service_guard_config.sql";
  }
}
