package io.harness.batch.processing.billing.service;

import java.math.BigDecimal;
import lombok.Value;

@Value
public class SystemCostData {
  private BigDecimal systemCost;
  private BigDecimal cpuSystemCost;
  private BigDecimal memorySystemCost;
}
