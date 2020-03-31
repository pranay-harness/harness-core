package migrations.timescaledb;

public class AddIdleUnallocatedColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_idle_unallocated_columns.sql";
  }
}
