package software.wings.graphql.schema.type.aggregation.instance;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;

@Value
@Builder
public class QLInstanceFilter {
  private QLInstanceFilterType type;
  private QLStringFilter stringFilter;
  private QLNumberFilter numberFilter;
}