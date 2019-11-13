package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

public enum QLCCMEntityGroupBy {
  Application(QLAggregationKind.SIMPLE),
  Service(QLAggregationKind.SIMPLE),
  Cluster(QLAggregationKind.SIMPLE),
  StartTime(QLAggregationKind.SIMPLE),
  Region(QLAggregationKind.SIMPLE),
  Environment(QLAggregationKind.SIMPLE);

  QLAggregationKind aggregationKind;

  QLCCMEntityGroupBy(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLCCMEntityGroupBy() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
