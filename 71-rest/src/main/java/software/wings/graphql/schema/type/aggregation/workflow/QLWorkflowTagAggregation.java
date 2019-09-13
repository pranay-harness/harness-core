package software.wings.graphql.schema.type.aggregation.workflow;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLWorkflowTagAggregation implements TagAggregation {
  private QLWorkflowTagType entityType;
  private String tagName;
}
