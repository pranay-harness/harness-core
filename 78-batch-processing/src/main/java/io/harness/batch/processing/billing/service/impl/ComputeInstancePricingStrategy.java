package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ComputeInstancePricingStrategy implements InstancePricingStrategy {
  private final VMPricingService vmPricingService;

  @Autowired
  public ComputeInstancePricingStrategy(VMPricingService vmPricingService) {
    this.vmPricingService = vmPricingService;
  }

  @Override
  public PricingData getPricePerHour(InstanceData instanceData) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    String instanceFamily = instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY);
    String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
    CloudProvider cloudProvider = CloudProvider.valueOf(instanceMetaData.get(InstanceMetaDataConstants.CLOUD_PROVIDER));
    VMComputePricingInfo vmComputePricingInfo =
        vmPricingService.getComputeVMPricingInfo(instanceFamily, region, cloudProvider);
    return PricingData.builder()
        .pricePerHour(vmComputePricingInfo.getOnDemandPrice())
        .cpuUnit(vmComputePricingInfo.getCpusPerVm() * 1024)
        .memoryMb(vmComputePricingInfo.getMemPerVm() * 1024)
        .build();
  }
}
