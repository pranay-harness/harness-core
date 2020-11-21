package io.harness.batch.processing.billing.service;

import java.math.BigDecimal;
import lombok.Value;

@Value
public class IdleCostData {
  private BigDecimal idleCost;
  private BigDecimal cpuIdleCost;
  private BigDecimal memoryIdleCost;
}
