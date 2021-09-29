package io.harness.batch.processing.pricing.vmpricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsFargatePricingInfo {
  private String region;
  private double cpuPrice;
  private double memoryPrice;
}
