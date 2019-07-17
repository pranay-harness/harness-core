package migrations.timescaledb;

public class InitVerificationSchemaMigration extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_verification.sql";
  }
}
