package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

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
