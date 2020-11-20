package software.wings.graphql.schema.type.aggregation.workflow;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLWorkflowAggregation implements Aggregation {
  private QLWorkflowEntityAggregation entityAggregation;
  private QLWorkflowTagAggregation tagAggregation;
}
