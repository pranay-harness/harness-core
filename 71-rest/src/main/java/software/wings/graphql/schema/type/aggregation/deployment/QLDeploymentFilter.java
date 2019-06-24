package software.wings.graphql.schema.type.aggregation.deployment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilterType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLDeploymentFilter implements QLStringFilterType, QLNumberFilterType {
  private QLDeploymentFilterType type;
  private QLStringFilter stringFilter;
  private QLNumberFilter numberFilter;

  @Override
  public String getFilterType() {
    return type != null ? type.name() : null;
  }
}
