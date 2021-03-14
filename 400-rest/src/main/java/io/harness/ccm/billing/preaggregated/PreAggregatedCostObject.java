package io.harness.ccm.billing.preaggregated;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._490_CE_COMMONS)
public class PreAggregatedCostObject {
  private double cost;
  private double trend;
}
