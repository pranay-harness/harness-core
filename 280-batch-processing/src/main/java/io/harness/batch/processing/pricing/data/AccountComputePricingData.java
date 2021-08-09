package io.harness.batch.processing.pricing.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountComputePricingData {
  private double blendedRate;
  private double blendedCost;
  private double unBlendedRate;
  private double unBlendedCost;
  private String availabilityZone;
  private String instanceType;
  private String operatingSystem;
  private String region;
  private double cpusPerVm;
  private double memPerVm;
}
