package io.harness.batch.processing.pricing.vmpricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VMInstanceBillingData {
  private String resourceId;
  private double computeCost;
  private double networkCost;
  private double cpuCost;
  private double memoryCost;
  private double rate;
}
