package software.wings.graphql.schema.type.aggregation.tag;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

/**
 * @author rktummala on 08/26/2019
 */
@Value
@Builder
public class QLTagAggregation {
  private QLEntityType entityType;
  private String tagName;
}
