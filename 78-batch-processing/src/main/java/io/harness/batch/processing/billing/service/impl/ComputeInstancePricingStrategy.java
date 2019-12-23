package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomPricingService;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class ComputeInstancePricingStrategy implements InstancePricingStrategy {
  private final VMPricingService vmPricingService;
  private final AwsCustomPricingService awsCustomPricingService;

  @Autowired
  public ComputeInstancePricingStrategy(
      VMPricingService vmPricingService, AwsCustomPricingService awsCustomPricingService) {
    this.vmPricingService = vmPricingService;
    this.awsCustomPricingService = awsCustomPricingService;
  }

  @Override
  public PricingData getPricePerHour(InstanceData instanceData, Instant startTime) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    CloudProvider cloudProvider = CloudProvider.valueOf(instanceMetaData.get(InstanceMetaDataConstants.CLOUD_PROVIDER));

    // TODO(Hitesh) check if cloud provider has s3 billing enabled
    VMComputePricingInfo vmComputePricingInfo = getCustomVMPricing(instanceData, startTime, cloudProvider);
    if (null == vmComputePricingInfo) {
      String instanceFamily = instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY);
      String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
      vmComputePricingInfo = vmPricingService.getComputeVMPricingInfo(instanceFamily, region, cloudProvider);
    }

    return PricingData.builder()
        .pricePerHour(vmComputePricingInfo.getOnDemandPrice())
        .cpuUnit(vmComputePricingInfo.getCpusPerVm() * 1024)
        .memoryMb(vmComputePricingInfo.getMemPerVm() * 1024)
        .build();
  }

  private VMComputePricingInfo getCustomVMPricing(
      InstanceData instanceData, Instant startTime, CloudProvider cloudProvider) {
    VMComputePricingInfo vmComputePricingInfo = null;
    if (cloudProvider == CloudProvider.AZURE) { // TODO(Hitesh) change name to AWS
      vmComputePricingInfo = awsCustomPricingService.getComputeVMPricingInfo(instanceData, startTime);
    }
    return vmComputePricingInfo;
  }
}
