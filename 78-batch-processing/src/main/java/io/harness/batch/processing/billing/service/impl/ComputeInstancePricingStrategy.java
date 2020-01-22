package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.data.ZonePrice;
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
  private static final String GCP_CUSTOM_INSTANCE_PREFIX = "custom-";

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

    if (instanceFamily.startsWith(GCP_CUSTOM_INSTANCE_PREFIX) && cloudProvider == CloudProvider.GCP) {
      return getGCPCustomInstancePricingData(instanceFamily, instanceCategory);
    }

    // TODO(Hitesh) check if cloud provider has s3 billing enabled
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

  private PricingData getGCPCustomInstancePricingData(String instanceFamily, InstanceCategory instanceCategory) {
    double cpuPricePerHr = 0.033174;
    double memoryPricePerHr = 0.004446;
    if (instanceCategory == InstanceCategory.SPOT) {
      cpuPricePerHr = 0.00698;
      memoryPricePerHr = 0.00094;
    }
    String[] split = instanceFamily.split("-");
    double cpu = Double.parseDouble(split[1]);
    double memory = Double.parseDouble(split[2]);
    double pricePerHr = (cpuPricePerHr * cpu) + ((memoryPricePerHr / 1024) * memory);
    return PricingData.builder().pricePerHour(pricePerHr).cpuUnit(cpu * 1024).memoryMb(memory).build();
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
    if (cloudProvider == CloudProvider.UNKNOWN) { // TODO(Hitesh) change name to AWS
      vmComputePricingInfo = awsCustomPricingService.getComputeVMPricingInfo(instanceData, startTime);
    }
    return vmComputePricingInfo;
  }
}
