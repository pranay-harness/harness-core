package software.wings.graphql.schema.type.aggregation;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLBillingStackedTimeSeriesData implements QLData {
  List<QLBillingStackedTimeSeriesDataPoint> data;
  List<QLBillingStackedTimeSeriesDataPoint> cpuIdleCost;
  List<QLBillingStackedTimeSeriesDataPoint> memoryIdleCost;
  List<QLBillingStackedTimeSeriesDataPoint> cpuUtilMetrics;
  List<QLBillingStackedTimeSeriesDataPoint> memoryUtilMetrics;
  String label;
}
