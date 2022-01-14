package io.harness.batch.processing.pricing.gcp.bigquery;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VMInstanceServiceBillingData {
  private double cost;
  private double rate;
  private Double effectiveCost;
  private String resourceId; // ProviderId for Azure
  private String serviceCode;
  private String productFamily; // MeterCategory for Azure
  private String usageType;
}
