package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLSinglePointData implements QLData {
  QLDataPoint dataPoint;
}
