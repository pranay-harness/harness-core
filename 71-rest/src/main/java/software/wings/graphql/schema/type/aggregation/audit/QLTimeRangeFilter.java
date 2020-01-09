package software.wings.graphql.schema.type.aggregation.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLTimeRange;

@Value
@Builder
public class QLTimeRangeFilter {
  private QLTimeRange specific;
  private QLRelativeTimeRange relative;
}
