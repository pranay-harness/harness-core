package software.wings.graphql.schema.type.aggregation.instance;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLInstanceFilter {
  private QLInstanceFilterType type;
  private String[] values;
}
