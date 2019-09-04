package software.wings.graphql.schema.type.aggregation.environment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLEnvironmentAggregation implements Aggregation {
  private QLEnvironmentEntityAggregation entityAggregation;
  private QLTagAggregation tagAggregation;
}