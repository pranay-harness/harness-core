/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
