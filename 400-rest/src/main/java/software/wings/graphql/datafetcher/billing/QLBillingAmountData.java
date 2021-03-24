package software.wings.graphql.datafetcher.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLBillingAmountData {
  private BigDecimal cost;
  private BigDecimal idleCost;
  private BigDecimal unallocatedCost;
  private long minStartTime;
  private long maxStartTime;
}
