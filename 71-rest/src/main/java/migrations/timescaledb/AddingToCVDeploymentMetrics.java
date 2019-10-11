package migrations.timescaledb;

public class AddingToCVDeploymentMetrics extends AbstractTimeSaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_fields_cv_worfklow_stats.sql";
  }
}
