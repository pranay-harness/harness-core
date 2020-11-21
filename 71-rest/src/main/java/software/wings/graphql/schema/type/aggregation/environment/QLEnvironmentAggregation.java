package software.wings.graphql.schema.type.aggregation.environment;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEnvironmentAggregation implements Aggregation {
  private QLEnvironmentEntityAggregation entityAggregation;
  private QLEnvironmentTagAggregation tagAggregation;
}
