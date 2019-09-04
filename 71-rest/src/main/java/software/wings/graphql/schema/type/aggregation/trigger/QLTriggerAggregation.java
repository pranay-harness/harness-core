package software.wings.graphql.schema.type.aggregation.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLTriggerAggregation implements Aggregation {
  private QLTriggerEntityAggregation entityAggregation;
  private QLTagAggregation tagAggregation;
}