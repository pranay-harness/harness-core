package migrations.timescaledb;

public class ChangeToTimeStampTZ extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/change_timestamp.sql";
  }
}
