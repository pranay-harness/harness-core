package io.harness.batch.processing.billing.service;

import io.harness.batch.processing.pricing.PricingSource;

import lombok.Value;

@Value
public class BillingData {
  private BillingAmountBreakup billingAmountBreakup;
  private IdleCostData idleCostData;
  private SystemCostData systemCostData;
  private double usageDurationSeconds;
  private double cpuUnitSeconds;
  private double memoryMbSeconds;
  private double storageMbSeconds;
  private double networkCost;
  private PricingSource pricingSource;
}
