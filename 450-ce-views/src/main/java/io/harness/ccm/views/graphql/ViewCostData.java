package io.harness.ccm.views.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewCostData {
  double cost;
  Double idleCost;
  Double unallocatedCost;
  Double systemCost;
  Double utilizedCost;
  long minStartTime;
  long maxStartTime;
}
