package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLChartDataPoint {
  long time;
  int eventsCount;
  int notableEventsCount;
}
