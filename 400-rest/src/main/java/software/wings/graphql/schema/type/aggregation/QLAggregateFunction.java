package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLAggregateFunction {
  String aggregateValue;
  QLAggregateOperation aggregateOperation;
}
