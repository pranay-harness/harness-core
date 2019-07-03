package software.wings.graphql.schema.type.aggregation.pipeline;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLPipelineFilter implements QLStringFilterType {
  private QLPipelineFilterType type;
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
