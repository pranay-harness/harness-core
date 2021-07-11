package io.harness.migrations.timescaledb;

public class AddSlackNotificationSupportAnomalies extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_slack_notification_to_anomalies.sql";
  }
}
