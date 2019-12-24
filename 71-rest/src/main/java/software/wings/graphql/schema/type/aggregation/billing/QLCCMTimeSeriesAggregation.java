package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCCMTimeSeriesAggregation {
  QLTimeGroupType timeGroupType;
}
