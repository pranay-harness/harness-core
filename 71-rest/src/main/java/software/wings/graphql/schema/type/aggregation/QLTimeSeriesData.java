package software.wings.graphql.schema.type.aggregation;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTimeSeriesData implements QLData {
  List<QLTimeSeriesDataPoint> dataPoints;
  String label;
}
