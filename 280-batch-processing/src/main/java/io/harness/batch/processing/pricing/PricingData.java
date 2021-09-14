/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class PricingData {
  private double networkCost;
  private double pricePerHour;
  private double cpuPricePerHour;
  private double memoryPricePerHour;
  private double cpuUnit;
  private double memoryMb;
  private double storageMb;
  private PricingSource pricingSource;
}
