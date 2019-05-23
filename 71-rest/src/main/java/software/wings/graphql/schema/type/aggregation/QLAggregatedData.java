package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLAggregatedData implements QLData {
  QLRequest request;
  List<QLDataPoint> dataPoints;
}
