package io.harness.batch.processing.pricing.vmpricing;

import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.pricing.dto.cloudinfo.ProductDetails;

import com.google.common.collect.ImmutableSet;

public interface VMPricingService {
  static String getSimilarRegionIfNotSupportedByBanzai(String region) {
    if (ImmutableSet.of("switzerlandnorth", "switzerlandwest", "germanywestcentral").contains(region)) {
      return "uksouth";
    }

    return region;
  }

  ProductDetails getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider);

  EcsFargatePricingInfo getFargatePricingInfo(String region);
}
