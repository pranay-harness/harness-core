package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreAggregatedCostData {
  private double cost;
  private long minStartTime;
  private long maxStartTime;
}