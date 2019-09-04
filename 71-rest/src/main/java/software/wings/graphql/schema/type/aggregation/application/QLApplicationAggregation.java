package software.wings.graphql.schema.type.aggregation.application;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLApplicationAggregation implements Aggregation {
  private QLApplicationEntityAggregation entityAggregation;
  private QLTagAggregation tagAggregation;
}