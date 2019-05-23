package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLStackedData implements QLData {
  List<QLStackedDataPoint> dataPoints;
  QLRequest request;
}
