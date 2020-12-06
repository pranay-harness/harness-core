package migrations.timescaledb;

public class CreateNewInstanceV2Migration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_instance_v2_table.sql";
  }
}
