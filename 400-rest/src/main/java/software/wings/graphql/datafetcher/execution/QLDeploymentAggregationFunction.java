/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.execution;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLCountAggregateOperation;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLDeploymentAggregationFunction {
  QLCountAggregateOperation count;
  QLDurationAggregateOperation duration;
  QLDurationAggregateOperation rollbackDuration;
  QLCountAggregateOperation instancesDeployed;
  QLCountAggregateOperation rollbackCount;
}
