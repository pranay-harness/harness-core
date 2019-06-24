package software.wings.graphql.schema.type.aggregation.environment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLEnvironmentFilter implements QLStringFilterType {
  private QLEnvironmentFilterType type;
  private QLStringFilter stringFilter;

  @Override
  public String getFilterType() {
    return type != null ? type.name() : null;
  }
}
