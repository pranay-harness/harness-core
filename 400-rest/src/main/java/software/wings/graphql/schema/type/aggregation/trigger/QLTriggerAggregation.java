package software.wings.graphql.schema.type.aggregation.trigger;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTriggerAggregation implements Aggregation {
  private QLTriggerEntityAggregation entityAggregation;
  private QLTriggerTagAggregation tagAggregation;
}
