package io.harness.migrations.timescaledb;

import io.harness.migrations.timescaledb.AbstractTimeScaleDBMigration;

public class OverviewDashboard extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_overview_dashboard_table.sql";
  }
}
