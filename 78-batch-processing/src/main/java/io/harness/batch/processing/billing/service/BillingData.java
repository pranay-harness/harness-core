package io.harness.batch.processing.billing.service;

import lombok.Value;

@Value
public class BillingData {
  private BillingAmountBreakup billingAmountBreakup;
  private IdleCostData idleCostData;
  private SystemCostData systemCostData;
  private double usageDurationSeconds;
  private double cpuUnitSeconds;
  private double memoryMbSeconds;
}
