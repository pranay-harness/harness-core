package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLStackedTimeSeriesData implements QLData {
  List<QLStackedTimeSeriesDataPoint> data;
  String label;
}
