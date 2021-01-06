package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLK8sWorkloadRecommendationPreset {
  Double cpuRequest;
  Double cpuLimit;
  Double memoryRequest;
  Double memoryLimit;
  Double safetyMargin;
  Long minCpuMilliCores;
  Long minMemoryBytes;
}
