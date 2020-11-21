package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala on 09/05/19
 */
@Value
@Builder
public class QLDeploymentTagAggregation implements TagAggregation {
  private QLDeploymentTagType entityType;
  private String tagName;
}
