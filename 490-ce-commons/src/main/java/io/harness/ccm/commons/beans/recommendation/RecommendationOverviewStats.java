package io.harness.ccm.commons.beans.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecommendationOverviewStats {
  double totalMonthlyCost;
  double totalMonthlySaving;
}
