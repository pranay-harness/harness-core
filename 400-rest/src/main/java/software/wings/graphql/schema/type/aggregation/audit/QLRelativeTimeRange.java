package software.wings.graphql.schema.type.aggregation.audit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLRelativeTimeRange {
  private QLTimeUnit timeUnit;
  private Long noOfUnits;
}
