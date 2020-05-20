package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.data.ZonePrice;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomPricingService;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.pricing.service.support.GCPCustomInstanceDetailProvider;
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
    String zone = instanceMetaData.get(InstanceMetaDataConstants.ZONE);
    InstanceCategory instanceCategory =
        InstanceCategory.valueOf(instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
    String instanceFamily = instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY);

    if (GCPCustomInstanceDetailProvider.isCustomGCPInstance(instanceFamily, cloudProvider)) {
      return GCPCustomInstanceDetailProvider.getGCPCustomInstancePricingData(instanceFamily, instanceCategory);
    } else if (cloudProvider == CloudProvider.IBM) {
      return getIBMInstancePricingData(instanceData);
    }

    VMComputePricingInfo vmComputePricingInfo = getCustomVMPricing(instanceData, startTime, cloudProvider);

    if (null == vmComputePricingInfo) {
      String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
      vmComputePricingInfo = vmPricingService.getComputeVMPricingInfo(instanceFamily, region, cloudProvider);
    }
    return PricingData.builder()
        .pricePerHour(getPricePerHour(zone, instanceCategory, vmComputePricingInfo))
        .cpuUnit(vmComputePricingInfo.getCpusPerVm() * 1024)
        .memoryMb(vmComputePricingInfo.getMemPerVm() * 1024)
        .build();
  }

  private PricingData getIBMInstancePricingData(InstanceData instanceData) {
    double cpuPricePerHr = 0.016;
    double memoryPricePerHr = 0.008;
    Double cpuUnits = instanceData.getTotalResource().getCpuUnits();
    Double memoryMb = instanceData.getTotalResource().getMemoryMb();
    if (instanceData.getInstanceType() == InstanceType.K8S_POD) {
      cpuUnits = Double.valueOf(instanceData.getMetaData().get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU));
      memoryMb = Double.valueOf(instanceData.getMetaData().get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY));
    }
    double pricePerHr = ((cpuPricePerHr * cpuUnits) / 1024) + ((memoryPricePerHr * memoryMb) / 1024);
    return PricingData.builder().pricePerHour(pricePerHr).cpuUnit(cpuUnits).memoryMb(memoryMb).build();
  }

  private double getPricePerHour(
      String zone, InstanceCategory instanceCategory, VMComputePricingInfo vmComputePricingInfo) {
    double pricePerHour = vmComputePricingInfo.getOnDemandPrice();
    if (instanceCategory == InstanceCategory.SPOT) {
      return vmComputePricingInfo.getSpotPrice()
          .stream()
          .filter(zonePrice -> zonePrice.getZone().equals(zone))
          .findFirst()
          .map(ZonePrice::getPrice)
          .orElse(pricePerHour);
    }
    return pricePerHour;
  }

  private VMComputePricingInfo getCustomVMPricing(
      InstanceData instanceData, Instant startTime, CloudProvider cloudProvider) {
    VMComputePricingInfo vmComputePricingInfo = null;
    if (cloudProvider == CloudProvider.UNKNOWN) {
      vmComputePricingInfo = awsCustomPricingService.getComputeVMPricingInfo(instanceData, startTime);
    }
    return vmComputePricingInfo;
  }
}
