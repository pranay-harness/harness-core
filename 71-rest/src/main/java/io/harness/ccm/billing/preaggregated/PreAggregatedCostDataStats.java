package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreAggregatedCostDataStats {
  PreAggregatedCostData unBlendedCost;
  PreAggregatedCostData blendedCost;
}
