package io.harness.batch.processing.pricing.service.intfc;

import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;

public interface VMPricingService {
  VMComputePricingInfo getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider);

  EcsFargatePricingInfo getFargatePricingInfo(String region);
}
