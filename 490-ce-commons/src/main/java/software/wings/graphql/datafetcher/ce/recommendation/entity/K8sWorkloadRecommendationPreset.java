package software.wings.graphql.datafetcher.ce.recommendation.entity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sWorkloadRecommendationPreset {
  // All percentiles are doubles in range (0, 1)

  // requests
  double cpuRequest;
  double memoryRequest;

  // limits (set <= 0 to omit)
  double cpuLimit;
  double memoryLimit;

  // Fraction to add on top
  double safetyMargin;
}
