package software.wings.graphql.datafetcher.billing;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLUnallocatedCost {
  private BigDecimal unallocatedCost;
  private BigDecimal cpuUnallocatedCost;
  private BigDecimal memoryUnallocatedCost;
}
