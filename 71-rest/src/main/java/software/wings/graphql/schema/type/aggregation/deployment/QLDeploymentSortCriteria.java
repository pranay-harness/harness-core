package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

@Value
@Builder
public class QLDeploymentSortCriteria {
  private QLDeploymentSortType sortType;
  private QLSortOrder sortOrder;
}
