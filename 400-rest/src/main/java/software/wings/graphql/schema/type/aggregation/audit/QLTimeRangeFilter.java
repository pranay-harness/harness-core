package software.wings.graphql.schema.type.aggregation.audit;

import software.wings.graphql.schema.type.aggregation.QLTimeRange;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTimeRangeFilter {
  private QLTimeRange specific;
  private QLRelativeTimeRange relative;
}
