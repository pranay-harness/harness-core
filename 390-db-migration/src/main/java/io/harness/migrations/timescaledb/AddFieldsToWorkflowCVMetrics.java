package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class AddFieldsToWorkflowCVMetrics extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_license_type_to_cv_workflow_stats.sql";
  }
}
