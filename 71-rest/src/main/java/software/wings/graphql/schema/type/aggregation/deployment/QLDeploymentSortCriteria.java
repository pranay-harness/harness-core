package software.wings.graphql.schema.type.aggregation.deployment;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDeploymentSortCriteria {
  private QLDeploymentSortType sortType;
  private QLSortOrder sortOrder;
}
