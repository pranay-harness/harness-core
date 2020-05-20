package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
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

  @Autowired
  public EcsFargateInstancePricingStrategy(VMPricingService vmPricingService) {
    this.vmPricingService = vmPricingService;
  }

  @Override
  public PricingData getPricePerHour(InstanceData instanceData, Instant startTime) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
    EcsFargatePricingInfo fargatePricingInfo = vmPricingService.getFargatePricingInfo(region);

    Double cpuUnits = instanceData.getTotalResource().getCpuUnits();
    Double memoryMb = instanceData.getTotalResource().getMemoryMb();
    if (InstanceType.K8S_POD == instanceData.getInstanceType()) {
      cpuUnits = Double.valueOf(instanceMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU));
      memoryMb = Double.valueOf(instanceMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY));
    }
    double cpuPricePerHour = (cpuUnits / 1024) * fargatePricingInfo.getCpuPrice();
    double memoryPricePerHour = (memoryMb / 1024) * fargatePricingInfo.getMemoryPrice();
    return PricingData.builder()
        .pricePerHour(cpuPricePerHour + memoryPricePerHour)
        .cpuUnit(cpuUnits)
        .memoryMb(memoryMb)
        .build();
  }
}
