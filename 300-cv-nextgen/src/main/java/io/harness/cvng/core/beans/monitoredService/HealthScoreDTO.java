package io.harness.cvng.core.beans.monitoredService;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HealthScoreDTO {
  RiskData currentHealthScore;
  // TODO: dependency health score to be added in here.
}
