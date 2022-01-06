/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
