package io.harness.batch.processing.pricing.service.intfc;

import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.ccm.commons.constants.CloudProvider;

public interface VMPricingService {
  VMComputePricingInfo getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider);

  EcsFargatePricingInfo getFargatePricingInfo(String region);
}
