package migrations.timescaledb;

public class AddIndexToInstanceV2Migration extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_index_to_instance_v2_table.sql";
  }
}
