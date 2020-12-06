package migrations.timescaledb;

public class RenameInstanceMigration extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/rename_instance_table.sql";
  }
}
