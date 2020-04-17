package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

public enum QLDeploymentEntityAggregation {
  Application(QLAggregationKind.SIMPLE),
  Service(QLAggregationKind.ARRAY),
  Environment(QLAggregationKind.ARRAY),
  EnvironmentType(QLAggregationKind.ARRAY),
  CloudProvider(QLAggregationKind.ARRAY),
  Status(QLAggregationKind.SIMPLE),
  TriggeredBy(QLAggregationKind.SIMPLE),
  Trigger(QLAggregationKind.SIMPLE),
  Workflow(QLAggregationKind.ARRAY),
  Pipeline(QLAggregationKind.SIMPLE),
  Deployment(QLAggregationKind.HSTORE);

  QLAggregationKind aggregationKind;

  QLDeploymentEntityAggregation(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLDeploymentEntityAggregation() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
