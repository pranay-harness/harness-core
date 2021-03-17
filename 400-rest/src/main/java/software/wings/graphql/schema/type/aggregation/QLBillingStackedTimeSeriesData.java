package software.wings.graphql.schema.type.aggregation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLBillingStackedTimeSeriesData implements QLData {
  List<QLBillingStackedTimeSeriesDataPoint> data;
  List<QLBillingStackedTimeSeriesDataPoint> cpuIdleCost;
  List<QLBillingStackedTimeSeriesDataPoint> memoryIdleCost;
  List<QLBillingStackedTimeSeriesDataPoint> cpuUtilMetrics;
  List<QLBillingStackedTimeSeriesDataPoint> memoryUtilMetrics;
  List<QLBillingStackedTimeSeriesDataPoint> cpuUtilValues;
  List<QLBillingStackedTimeSeriesDataPoint> memoryUtilValues;
  List<QLBillingStackedTimeSeriesDataPoint> cpuRequest;
  List<QLBillingStackedTimeSeriesDataPoint> cpuLimit;
  List<QLBillingStackedTimeSeriesDataPoint> memoryRequest;
  List<QLBillingStackedTimeSeriesDataPoint> memoryLimit;
  String label;
  String info;
}
