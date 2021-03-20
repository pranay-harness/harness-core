package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
