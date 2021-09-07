package io.harness.migrations.timescaledb;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CE)
public class RecommendationsRelatedTables extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/create_pod_node_info_recommendation_table.sql";
  }
}
