package software.wings.graphql.datafetcher.execution;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLCountAggregateOperation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLDeploymentAggregationFunction {
  QLCountAggregateOperation count;
  QLDurationAggregateOperation duration;
  QLDurationAggregateOperation rollbackDuration;
  QLCountAggregateOperation instancesDeployed;
}
