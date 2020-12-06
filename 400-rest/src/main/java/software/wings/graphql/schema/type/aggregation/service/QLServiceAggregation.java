package software.wings.graphql.schema.type.aggregation.service;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLServiceAggregation implements Aggregation {
  private QLServiceEntityAggregation entityAggregation;
  private QLServiceTagAggregation tagAggregation;
}
