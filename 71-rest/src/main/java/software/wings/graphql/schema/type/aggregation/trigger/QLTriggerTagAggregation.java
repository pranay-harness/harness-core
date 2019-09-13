package software.wings.graphql.schema.type.aggregation.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLTriggerTagAggregation implements TagAggregation<QLTriggerTagType> {
  private QLTriggerTagType entityType;
  private String tagName;
}
