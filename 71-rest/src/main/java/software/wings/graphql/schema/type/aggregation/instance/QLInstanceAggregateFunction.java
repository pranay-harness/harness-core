package software.wings.graphql.schema.type.aggregation.instance;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLCountAggregateOperation;

@Value
@Builder
public class QLInstanceAggregateFunction {
  QLCountAggregateOperation count;
}
