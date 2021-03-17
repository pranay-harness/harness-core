package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLEventData implements QLData {
  List<QLEventsDataPoint> data;
  List<QLChartDataPoint> chartData;
}
