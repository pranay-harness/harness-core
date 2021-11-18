package io.harness.batch.processing.pricing.vmpricing;

import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.pricing.dto.cloudinfo.ProductDetails;

import com.google.common.collect.ImmutableSet;

public interface VMPricingService {
  static String getSimilarRegionIfNotSupportedByBanzai(String region) {
    // these region names are present in Azure cloud Provider Only
    if (ImmutableSet.of("switzerlandnorth", "switzerlandwest", "germanywestcentral", "norwayeast").contains(region)) {
      return "uksouth";
    }

    return region;
  }

  ProductDetails getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider);

  EcsFargatePricingInfo getFargatePricingInfo(String region);
}
