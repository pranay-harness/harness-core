package io.harness.batch.processing.billing.service;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class SystemCostData {
  private BigDecimal systemCost;
  private BigDecimal cpuSystemCost;
  private BigDecimal memorySystemCost;
}
