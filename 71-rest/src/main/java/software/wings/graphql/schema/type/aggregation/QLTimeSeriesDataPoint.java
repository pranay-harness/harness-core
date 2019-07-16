package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTimeSeriesDataPoint {
  Number value;
  Long time;
}
