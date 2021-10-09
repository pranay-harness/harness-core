package io.harness.migrations.timescaledb;

public class AddFeedbackToAnomalies extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_feedback_to_anomalies.sql";
  }
}
