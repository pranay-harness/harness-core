package io.harness.batch.processing.pricing.service.intfc;

import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;

import java.time.Instant;
import java.util.List;

public interface AwsCustomBillingService {
  VMInstanceBillingData getComputeVMPricingInfo(InstanceData instanceData, Instant startTime, Instant endTime);

  void updateAwsEC2BillingDataCache(List<String> resourceIds, Instant startTime, Instant endTime, String dataSetId);

  EcsFargatePricingInfo getFargateVMPricingInfo(InstanceData instanceData, Instant startTime);
}
