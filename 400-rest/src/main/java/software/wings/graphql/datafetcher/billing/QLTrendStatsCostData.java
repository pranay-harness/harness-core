package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
public class QLTrendStatsCostData {
  QLBillingAmountData totalCostData;
  QLBillingAmountData idleCostData;
  QLBillingAmountData unallocatedCostData;
  QLBillingAmountData systemCostData;
}
