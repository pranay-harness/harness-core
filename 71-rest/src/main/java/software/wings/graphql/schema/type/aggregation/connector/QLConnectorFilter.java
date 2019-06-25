package software.wings.graphql.schema.type.aggregation.connector;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilterType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLConnectorFilter implements QLStringFilterType, QLNumberFilterType {
  private QLConnectorFilterType type;
  private QLStringFilter stringFilter;
  private QLNumberFilter numberFilter;

  @Override
  public String getFilterType() {
    return type != null ? type.name() : null;
  }

  @Override
  public QLDataType getDataType() {
    return type != null ? type.getDataType() : null;
  }
}