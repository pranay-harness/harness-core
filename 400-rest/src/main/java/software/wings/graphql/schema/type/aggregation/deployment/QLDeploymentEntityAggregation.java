/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
