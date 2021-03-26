package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLChartDataPoint {
  long time;
  int eventsCount;
  int notableEventsCount;
}
