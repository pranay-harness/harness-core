package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class QLUnallocatedCost {
  private BigDecimal unallocatedCost;
  private BigDecimal cpuUnallocatedCost;
  private BigDecimal memoryUnallocatedCost;
}
