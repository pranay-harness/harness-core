package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class QLBillingAmountData {
  private BigDecimal cost;
  private BigDecimal idleCost;
  private BigDecimal unallocatedCost;
  private long minStartTime;
  private long maxStartTime;
}
