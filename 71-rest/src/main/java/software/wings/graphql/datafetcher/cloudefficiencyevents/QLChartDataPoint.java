package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLChartDataPoint {
  long time;
  int eventsCount;
  int notableEventsCount;
}
