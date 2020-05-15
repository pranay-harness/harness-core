package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreAggregateCloudOverviewDataPoint {
  String name;
  Number cost;
  Number trend;
}
