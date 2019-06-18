package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;

@Value
@Builder
public class QLDeploymentFilter {
  private QLDeploymentFilterType type;
  private QLStringFilter stringFilter;
  private QLNumberFilter numberFilter;
}
