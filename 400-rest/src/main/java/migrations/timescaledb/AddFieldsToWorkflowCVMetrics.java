package migrations.timescaledb;

public class AddFieldsToWorkflowCVMetrics extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_license_type_to_cv_workflow_stats.sql";
  }
}
