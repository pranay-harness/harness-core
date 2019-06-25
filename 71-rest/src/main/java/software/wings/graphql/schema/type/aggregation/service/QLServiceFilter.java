package software.wings.graphql.schema.type.aggregation.service;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLServiceFilter implements QLStringFilterType {
  private QLServiceFilterType type;
  private QLStringFilter stringFilter;

  @Override
  public String getFilterType() {
    return type != null ? type.name() : null;
  }

  @Override
  public QLDataType getDataType() {
    return type != null ? type.getDataType() : null;
  }
}
