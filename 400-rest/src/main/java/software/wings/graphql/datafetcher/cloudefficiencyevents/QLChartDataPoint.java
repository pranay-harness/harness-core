package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QLChartDataPoint {
  long time;
  int eventsCount;
  int notableEventsCount;
}
