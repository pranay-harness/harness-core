package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
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
public class EcsFargateInstancePricingStrategy implements InstancePricingStrategy {
  private final VMPricingService vmPricingService;
  private final AwsCustomPricingService awsCustomPricingService;

  @Autowired
  public EcsFargateInstancePricingStrategy(
      VMPricingService vmPricingService, AwsCustomPricingService awsCustomPricingService) {
    this.vmPricingService = vmPricingService;
    this.awsCustomPricingService = awsCustomPricingService;
  }

  @Override
  public PricingData getPricePerHour(InstanceData instanceData, Instant startTime) {
    EcsFargatePricingInfo fargatePricingInfo = awsCustomPricingService.getFargateVMPricingInfo(instanceData, startTime);
    if (null == fargatePricingInfo) {
      Map<String, String> instanceMetaData = instanceData.getMetaData();
      String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
      fargatePricingInfo = vmPricingService.getFargatePricingInfo(region);
    }

    double cpuPricePerHour = (instanceData.getTotalResource().getCpuUnits() / 1024) * fargatePricingInfo.getCpuPrice();
    double memoryPricePerHour =
        (instanceData.getTotalResource().getMemoryMb() / 1024) * fargatePricingInfo.getMemoryPrice();
    return PricingData.builder()
        .pricePerHour(cpuPricePerHour + memoryPricePerHour)
        .cpuUnit(instanceData.getTotalResource().getCpuUnits())
        .memoryMb(instanceData.getTotalResource().getMemoryMb())
        .build();
  }
}
