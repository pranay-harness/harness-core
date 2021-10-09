package io.harness.batch.processing.pricing.vmpricing;

import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.pricing.dto.cloudinfo.ProductDetails;

public interface VMPricingService {
  ProductDetails getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider);

  EcsFargatePricingInfo getFargatePricingInfo(String region);
}
