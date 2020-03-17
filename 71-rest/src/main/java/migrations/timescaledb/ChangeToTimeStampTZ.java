package migrations.timescaledb;

public class ChangeToTimeStampTZ extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/change_timestamp.sql";
  }
}
