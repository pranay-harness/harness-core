package migrations.timescaledb;

public class InitSchemaMigration extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_script.sql";
  }
}
