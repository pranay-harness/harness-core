package io.harness.migrations.timescaledb.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.TimeScaleDBDataMigration;
import io.harness.migrations.timescaledb.AbstractTimeScaleDBMigration;

@OwnedBy(HarnessTeam.CE)
public class OptimizeNodeRecommendationQuery extends AbstractTimeScaleDBMigration implements TimeScaleDBDataMigration {
  @Override
  public String getFileName() {
    return "timescaledb/optimize_node_recommendation_query.sql";
  }
}
