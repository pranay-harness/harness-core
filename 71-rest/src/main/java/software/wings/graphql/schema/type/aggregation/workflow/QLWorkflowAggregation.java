package software.wings.graphql.schema.type.aggregation.workflow;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLWorkflowAggregation implements Aggregation {
  private QLWorkflowEntityAggregation entityAggregation;
  private QLTagAggregation tagAggregation;
}