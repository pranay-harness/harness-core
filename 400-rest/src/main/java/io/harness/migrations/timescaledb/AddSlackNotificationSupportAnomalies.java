package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._390_DB_MIGRATION)
public class AddSlackNotificationSupportAnomalies extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_slack_notification_to_anomalies.sql";
  }
}
