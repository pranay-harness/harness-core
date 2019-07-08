package software.wings.graphql.schema.type.aggregation.instance;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilterType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLInstanceFilter implements QLStringFilterType, QLNumberFilterType {
  private QLInstanceFilterType type;
  private QLStringFilter stringFilter;
  private QLNumberFilter numberFilter;

  @Override
  public QLDataType getDataType() {
    return type.getDataType();
  }

  public String getFilterType() {
    return type != null ? type.name() : null;
  }
}