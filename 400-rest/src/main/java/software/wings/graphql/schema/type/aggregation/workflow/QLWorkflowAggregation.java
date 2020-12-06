package software.wings.graphql.schema.type.aggregation.workflow;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLWorkflowAggregation implements Aggregation {
  private QLWorkflowEntityAggregation entityAggregation;
  private QLWorkflowTagAggregation tagAggregation;
}
