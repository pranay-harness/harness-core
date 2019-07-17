package migrations.timescaledb;

public class RenameInstanceMigration extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/rename_instance_table.sql";
  }
}
