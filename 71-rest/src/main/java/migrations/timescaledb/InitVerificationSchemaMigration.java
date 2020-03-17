package migrations.timescaledb;

public class InitVerificationSchemaMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/seed_verification.sql";
  }
}
