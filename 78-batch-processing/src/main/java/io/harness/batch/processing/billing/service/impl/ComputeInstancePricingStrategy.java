package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.PricingSource;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.data.ZonePrice;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.pricing.service.support.GCPCustomInstanceDetailProvider;
import io.harness.batch.processing.processor.util.InstanceMetaDataUtils;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class ComputeInstancePricingStrategy implements InstancePricingStrategy {
  private final VMPricingService vmPricingService;
  private final AwsCustomBillingService awsCustomBillingService;
  private final InstanceResourceService instanceResourceService;
  private final EcsFargateInstancePricingStrategy ecsFargateInstancePricingStrategy;
  private final CustomBillingMetaDataService customBillingMetaDataService;

  @Autowired
  public ComputeInstancePricingStrategy(VMPricingService vmPricingService,
      AwsCustomBillingService awsCustomBillingService, InstanceResourceService instanceResourceService,
      EcsFargateInstancePricingStrategy ecsFargateInstancePricingStrategy,
      CustomBillingMetaDataService customBillingMetaDataService) {
    this.vmPricingService = vmPricingService;
    this.awsCustomBillingService = awsCustomBillingService;
    this.instanceResourceService = instanceResourceService;
    this.ecsFargateInstancePricingStrategy = ecsFargateInstancePricingStrategy;
    this.customBillingMetaDataService = customBillingMetaDataService;
  }

  @Override
  public PricingData getPricePerHour(
      InstanceData instanceData, Instant startTime, Instant endTime, double instanceActiveSeconds) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    CloudProvider cloudProvider = CloudProvider.valueOf(instanceMetaData.get(InstanceMetaDataConstants.CLOUD_PROVIDER));
    String zone = instanceMetaData.get(InstanceMetaDataConstants.ZONE);
    InstanceCategory instanceCategory =
        InstanceCategory.valueOf(instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
    String instanceFamily = instanceMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY);
    String computeType = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.COMPUTE_TYPE, instanceMetaData);
    String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
    PricingData customVMPricing = getCustomVMPricing(
        instanceData, startTime, endTime, instanceActiveSeconds, instanceFamily, region, cloudProvider);

    if (null == customVMPricing) {
      if (GCPCustomInstanceDetailProvider.isCustomGCPInstance(instanceFamily, cloudProvider)) {
        return GCPCustomInstanceDetailProvider.getGCPCustomInstancePricingData(instanceFamily, instanceCategory);
      } else if (cloudProvider == CloudProvider.IBM) {
        return getIBMInstancePricingData(instanceData);
      } else if (cloudProvider == CloudProvider.AWS && K8sCCMConstants.AWS_FARGATE_COMPUTE_TYPE.equals(computeType)) {
        return ecsFargateInstancePricingStrategy.getPricePerHour(
            instanceData, startTime, endTime, instanceActiveSeconds);
      }

      VMComputePricingInfo vmComputePricingInfo =
          vmPricingService.getComputeVMPricingInfo(instanceFamily, region, cloudProvider);
      return PricingData.builder()
          .pricePerHour(getPricePerHour(zone, instanceCategory, vmComputePricingInfo))
          .cpuUnit(vmComputePricingInfo.getCpusPerVm() * 1024)
          .memoryMb(vmComputePricingInfo.getMemPerVm() * 1024)
          .build();
    }
    return customVMPricing;
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

  private PricingData getCustomVMPricing(InstanceData instanceData, Instant startTime, Instant endTime,
      double instanceActiveSeconds, String instanceFamily, String region, CloudProvider cloudProvider) {
    Resource computeVMResource = instanceResourceService.getComputeVMResource(instanceFamily, region, cloudProvider);
    PricingData pricingData = null;
    String awsDataSetId = customBillingMetaDataService.getAwsDataSetId(instanceData.getAccountId());
    if (cloudProvider == CloudProvider.AWS && null != awsDataSetId) {
      VMInstanceBillingData vmInstanceBillingData =
          awsCustomBillingService.getComputeVMPricingInfo(instanceData, startTime, endTime);
      if (null != vmInstanceBillingData) {
        double pricePerHr = (vmInstanceBillingData.getComputeCost() * 3600) / instanceActiveSeconds;
        pricingData = PricingData.builder()
                          .pricePerHour(pricePerHr)
                          .networkCost(vmInstanceBillingData.getNetworkCost())
                          .pricingSource(PricingSource.CUR_REPORT)
                          .cpuUnit(computeVMResource.getCpuUnits())
                          .memoryMb(computeVMResource.getMemoryMb())
                          .build();
      }
    }
    return pricingData;
  }
}
