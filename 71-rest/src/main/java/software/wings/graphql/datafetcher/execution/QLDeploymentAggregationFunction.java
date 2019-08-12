package software.wings.graphql.datafetcher.execution;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLCountAggregateOperation;

@Value
@Builder
public class QLDeploymentAggregationFunction {
  QLCountAggregateOperation count;
  QLDurationAggregateOperation duration;
  QLDurationAggregateOperation rollbackDuration;
}
