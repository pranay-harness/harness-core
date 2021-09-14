/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLCEEntityGroupBy {
  Application(QLAggregationKind.SIMPLE),
  Service(QLAggregationKind.SIMPLE),
  Cluster(QLAggregationKind.SIMPLE), // for cluster id
  Region(QLAggregationKind.SIMPLE),
  Environment(QLAggregationKind.SIMPLE),
  EcsService(QLAggregationKind.SIMPLE),
  Task(QLAggregationKind.SIMPLE),
  LaunchType(QLAggregationKind.SIMPLE),
  Workload(QLAggregationKind.SIMPLE),
  WorkloadType(QLAggregationKind.SIMPLE),
  Namespace(QLAggregationKind.SIMPLE),
  Node(QLAggregationKind.SIMPLE),
  Pod(QLAggregationKind.SIMPLE);

  QLAggregationKind aggregationKind;

  QLCEEntityGroupBy(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLCEEntityGroupBy() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
