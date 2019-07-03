package software.wings.graphql.schema.type.aggregation.workflow;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLWorkflowFilter implements QLStringFilterType {
  private QLWorkflowFilterType type;
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
