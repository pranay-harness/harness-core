package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PreAggregateCloudOverviewDataPoint {
  String name;
  Number cost;
  Number trend;
}
