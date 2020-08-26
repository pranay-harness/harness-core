package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEventsDataPoint {
  String clusterId;
  String clusterName;
  String namespace;
  String workloadName;
  String source;
  String type;
  String eventPriorityType;
  String details;
  long time;
  String oldYamlRef;
  String newYamlRef;
  Double costChangePercentage;
}
