package software.wings.graphql.schema.type.aggregation.environment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLEnvironmentAggregation implements Aggregation {
  private QLEnvironmentEntityAggregation entityAggregation;
  private QLEnvironmentTagAggregation tagAggregation;
}
