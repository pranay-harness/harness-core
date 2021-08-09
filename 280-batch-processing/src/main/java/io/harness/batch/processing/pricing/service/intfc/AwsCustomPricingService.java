package io.harness.batch.processing.pricing.service.intfc;

import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.ccm.commons.entities.batch.InstanceData;

import java.time.Instant;

public interface AwsCustomPricingService {
  VMComputePricingInfo getComputeVMPricingInfo(InstanceData instanceData, Instant startTime);

  EcsFargatePricingInfo getFargateVMPricingInfo(InstanceData instanceData, Instant startTime);
}
