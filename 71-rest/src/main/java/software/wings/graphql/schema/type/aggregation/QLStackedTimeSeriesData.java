package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLStackedTimeSeriesData implements QLData {
  List<QLStackedTimeSeriesDataPoint> data;
  String label;
  QLRequest request;
}
