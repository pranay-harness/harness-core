package software.wings.graphql.schema.type.aggregation.pipeline;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLPipelineTagAggregation implements TagAggregation {
  private QLPipelineTagType entityType;
  private String tagName;
}
