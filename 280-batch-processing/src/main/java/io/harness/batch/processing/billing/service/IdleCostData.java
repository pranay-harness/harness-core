package io.harness.batch.processing.billing.service;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class IdleCostData {
  private BigDecimal idleCost;
  private BigDecimal cpuIdleCost;
  private BigDecimal memoryIdleCost;
}
