package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class PreAggregatedCostDataStats {
  PreAggregatedCostData unBlendedCost;
  PreAggregatedCostData blendedCost;
  PreAggregatedCostData cost;
}
