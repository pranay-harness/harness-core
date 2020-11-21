package io.harness.batch.processing.billing.service;

import io.harness.batch.processing.ccm.PricingSource;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PricingData {
  private double networkCost;
  private double pricePerHour;
  private double cpuUnit;
  private double memoryMb;
  private PricingSource pricingSource;
}
