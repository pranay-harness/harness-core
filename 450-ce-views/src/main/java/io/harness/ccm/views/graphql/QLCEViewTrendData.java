package io.harness.ccm.views.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCEViewTrendData {
  QLCEViewTrendInfo totalCost;
  QLCEViewTrendInfo idleCost;
  QLCEViewTrendInfo unallocatedCost;
  QLCEViewTrendInfo utilizedCost;
  QLCEViewTrendInfo systemCost;
  EfficiencyScoreStats efficiencyScoreStats;
}
