package io.harness.batch.processing.billing.service;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class BillingData {
  private BigDecimal billingAmount;
  private IdleCostData idleCostData;
  private double usageDurationSeconds;
  private double cpuUnitSeconds;
  private double memoryMbSeconds;
}
