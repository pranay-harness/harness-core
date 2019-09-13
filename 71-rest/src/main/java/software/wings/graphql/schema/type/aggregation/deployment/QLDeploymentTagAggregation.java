package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLDeploymentTagAggregation implements TagAggregation {
  private QLDeploymentTagType entityType;
  private String tagName;
}
