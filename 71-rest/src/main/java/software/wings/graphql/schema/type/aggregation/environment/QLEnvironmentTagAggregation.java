package software.wings.graphql.schema.type.aggregation.environment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLEnvironmentTagAggregation implements TagAggregation {
  private QLEnvironmentTagType entityType;
  private String tagName;
}
