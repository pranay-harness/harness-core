package software.wings.graphql.schema.type.aggregation.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;

@Value
@Builder
public class QLTriggerFilter implements QLStringFilterType {
  private QLTriggerFilterType type;
  private QLStringFilter stringFilter;

  @Override
  public String getFilterType() {
    return type != null ? type.name() : null;
  }
}
