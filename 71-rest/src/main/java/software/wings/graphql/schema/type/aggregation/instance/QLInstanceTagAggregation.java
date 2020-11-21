package software.wings.graphql.schema.type.aggregation.instance;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLInstanceTagAggregation implements TagAggregation {
  private QLInstanceTagType entityType;
  private String tagName;
}
