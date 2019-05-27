package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeploymentFilter {
  private QLDeploymentFilterType type;
  private String[] values;
}
