package software.wings.graphql.schema.type.aggregation.pipeline;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;

@Value
@Builder
public class QLPipelineAggregation implements Aggregation {
  private QLPipelineEntityAggregation entityAggregation;
  private QLPipelineTagAggregation tagAggregation;
}
