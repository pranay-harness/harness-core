package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTimeSeriesAggregation {
  QLTimeAggregationType timeAggregationType;
  Integer timeAggregationValue;
}
