package software.wings.graphql.datafetcher.execution;

import software.wings.graphql.schema.type.aggregation.QLCountAggregateOperation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeploymentAggregationFunction {
  QLCountAggregateOperation count;
  QLDurationAggregateOperation duration;
  QLDurationAggregateOperation rollbackDuration;
  QLCountAggregateOperation instancesDeployed;
}
