package software.wings.graphql.datafetcher.billing;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLBillingAmountData {
  private BigDecimal cost;
  private BigDecimal idleCost;
  private BigDecimal unallocatedCost;
  private long minStartTime;
  private long maxStartTime;
}
