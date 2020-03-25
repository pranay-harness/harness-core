package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEventsDataPoint {
  String source;
  String type;
  String details;
  long time;
  String oldYamlRef;
  String newYamlRef;
}
