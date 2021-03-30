package io.harness.batch.processing.pricing.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VMInstanceServiceBillingData {
  private double cost;
  private Double effectiveCost;
  private String resourceId; // ProviderId for Azure
  private String serviceCode;
  private String productFamily; // MeterCategory for Azure
  private String usageType;
}
