package software.wings.graphql.schema.type.aggregation.pipeline;

import software.wings.graphql.schema.type.aggregation.Aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipelineAggregation implements Aggregation {
  private QLPipelineEntityAggregation entityAggregation;
  private QLPipelineTagAggregation tagAggregation;
}
