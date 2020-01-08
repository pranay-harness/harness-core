package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

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
  CloudProvider(QLAggregationKind.SIMPLE);

  QLAggregationKind aggregationKind;

  QLCCMEntityGroupBy(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLCCMEntityGroupBy() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
