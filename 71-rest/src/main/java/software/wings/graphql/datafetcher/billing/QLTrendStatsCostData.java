package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTrendStatsCostData {
  QLBillingAmountData totalCostData;
  QLBillingAmountData idleCostData;
  QLBillingAmountData unallocatedCostData;
}
