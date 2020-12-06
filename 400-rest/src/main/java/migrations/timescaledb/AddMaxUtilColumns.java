package migrations.timescaledb;

public class AddMaxUtilColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_maxutil_column_k8s_util_table.sql";
  }
}
