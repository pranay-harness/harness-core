package io.harness.batch.processing.billing.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PricingData {
  private double pricePerHour;
  private double cpuUnit;
  private double memoryMb;
}
