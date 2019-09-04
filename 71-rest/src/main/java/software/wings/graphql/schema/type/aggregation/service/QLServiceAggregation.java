package software.wings.graphql.schema.type.aggregation.service;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLServiceAggregation implements Aggregation {
  private QLServiceEntityAggregation entityAggregation;
  private QLTagAggregation tagAggregation;
}