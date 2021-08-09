package software.wings.service.impl.analysis;

import lombok.Builder;

@Builder
public class ExperimentPerformance {
  Double improvementPercentage;
  Double declinePercentage;
}
