package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

public enum QLDeploymentAggregation {
  Application(QLAggregationKind.SIMPLE),
  Service(QLAggregationKind.ARRAY),
  Environment(QLAggregationKind.ARRAY),
  CloudProvider(QLAggregationKind.ARRAY),
  Status(QLAggregationKind.SIMPLE),
  TriggeredBy(QLAggregationKind.SIMPLE),
  Trigger(QLAggregationKind.SIMPLE),
  Workflow(QLAggregationKind.ARRAY),
  Pipeline(QLAggregationKind.SIMPLE);

  QLAggregationKind aggregationKind;

  QLDeploymentAggregation(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLDeploymentAggregation() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
