package migrations.timescaledb;

public class DeploymentAdditionalColumns extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/deployment_table_add_cols.sql";
  }
}
