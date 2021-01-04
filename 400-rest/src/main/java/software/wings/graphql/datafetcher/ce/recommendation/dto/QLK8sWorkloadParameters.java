package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLK8sWorkloadParameters {
  String cluster;
  String namespace;
  String workloadName;
  String workloadType;
  Long startDate;
  Long endDate;
}
