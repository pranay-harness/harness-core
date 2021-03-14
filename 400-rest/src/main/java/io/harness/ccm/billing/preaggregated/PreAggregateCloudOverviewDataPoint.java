package io.harness.ccm.billing.preaggregated;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._490_CE_COMMONS)
public class PreAggregateCloudOverviewDataPoint {
  String name;
  Number cost;
  Number trend;
}
