package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class QLIdleCostData {
  private BigDecimal totalCost;
  private BigDecimal totalCpuCost;
  private BigDecimal totalMemoryCost;
  private BigDecimal idleCost;
  private BigDecimal cpuIdleCost;
  private BigDecimal memoryIdleCost;
  private BigDecimal avgCpuUtilization;
  private BigDecimal avgMemoryUtilization;
  private long minStartTime;
  private long maxStartTime;
}
