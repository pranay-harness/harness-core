/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.vmpricing;

import io.harness.batch.processing.pricing.banzai.VMComputePricingInfo;
import io.harness.ccm.commons.constants.CloudProvider;

public interface VMPricingService {
  VMComputePricingInfo getComputeVMPricingInfo(String instanceType, String region, CloudProvider cloudProvider);

  EcsFargatePricingInfo getFargatePricingInfo(String region);
}
