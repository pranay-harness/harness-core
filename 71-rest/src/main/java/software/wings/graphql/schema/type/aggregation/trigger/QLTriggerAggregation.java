package software.wings.graphql.schema.type.aggregation.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLTriggerAggregation implements Aggregation {
  private QLTriggerEntityAggregation entityAggregation;
  private QLTriggerTagAggregation tagAggregation;
}
