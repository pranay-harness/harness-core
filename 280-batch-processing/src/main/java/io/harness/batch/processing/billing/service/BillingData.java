/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
