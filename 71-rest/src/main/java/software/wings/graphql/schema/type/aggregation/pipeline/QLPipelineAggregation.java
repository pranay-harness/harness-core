package software.wings.graphql.schema.type.aggregation.pipeline;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Aggregation;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;

@Value
@Builder
public class QLPipelineAggregation implements Aggregation {
  private QLPipelineEntityAggregation entityAggregation;
  private QLTagAggregation tagAggregation;
}