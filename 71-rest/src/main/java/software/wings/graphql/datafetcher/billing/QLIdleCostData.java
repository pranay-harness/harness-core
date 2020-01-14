package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
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
