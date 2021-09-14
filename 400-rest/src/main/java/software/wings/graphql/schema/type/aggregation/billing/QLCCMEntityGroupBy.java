/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLCCMEntityGroupBy {
  Application(QLAggregationKind.SIMPLE),
  Service(QLAggregationKind.SIMPLE),
  Cluster(QLAggregationKind.SIMPLE), // for cluster id
  StartTime(QLAggregationKind.SIMPLE),
  Region(QLAggregationKind.SIMPLE),
  Environment(QLAggregationKind.SIMPLE),
  CloudServiceName(QLAggregationKind.SIMPLE),
  TaskId(QLAggregationKind.SIMPLE),
  LaunchType(QLAggregationKind.SIMPLE),
  WorkloadName(QLAggregationKind.SIMPLE),
  WorkloadType(QLAggregationKind.SIMPLE),
  Namespace(QLAggregationKind.SIMPLE),
  ClusterType(QLAggregationKind.SIMPLE),
  ClusterName(QLAggregationKind.SIMPLE),
  InstanceType(QLAggregationKind.SIMPLE),
  InstanceName(QLAggregationKind.SIMPLE),
  CloudProvider(QLAggregationKind.SIMPLE),
  Node(QLAggregationKind.SIMPLE),
  Pod(QLAggregationKind.SIMPLE),
  PV(QLAggregationKind.SIMPLE);

  QLAggregationKind aggregationKind;

  QLCCMEntityGroupBy(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLCCMEntityGroupBy() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
