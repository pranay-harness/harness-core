package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeploymentTag {
  private String name;
  private String value;
}
