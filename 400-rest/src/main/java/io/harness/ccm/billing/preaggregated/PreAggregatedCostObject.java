package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreAggregatedCostObject {
  private double cost;
  private double trend;
}
