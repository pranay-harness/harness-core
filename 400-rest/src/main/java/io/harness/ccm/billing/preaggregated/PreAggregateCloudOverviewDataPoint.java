package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class PreAggregateCloudOverviewDataPoint {
  String name;
  Number cost;
  Number trend;
}
