package io.harness.batch.processing.pricing.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountFargatePricingData {
  private double blendedRate;
  private double blendedCost;
  private double unBlendedRate;
  private double unBlendedCost;
  private String region;
  private boolean cpuPriceType;
  private boolean memoryPriceType;
}
