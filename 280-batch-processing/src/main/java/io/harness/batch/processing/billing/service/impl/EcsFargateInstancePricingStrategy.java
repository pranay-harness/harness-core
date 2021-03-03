package io.harness.batch.processing.billing.service.impl;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.tasklet.util.K8sResourceUtils;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.entities.InstanceData;

import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EcsFargateInstancePricingStrategy implements InstancePricingStrategy {
  private final VMPricingService vmPricingService;

  @Autowired
  public EcsFargateInstancePricingStrategy(VMPricingService vmPricingService) {
    this.vmPricingService = vmPricingService;
  }

  @Override
  public PricingData getPricePerHour(
      InstanceData instanceData, Instant startTime, Instant endTime, double instanceActiveSeconds) {
    Map<String, String> instanceMetaData = instanceData.getMetaData();
    String region = instanceMetaData.get(InstanceMetaDataConstants.REGION);
    EcsFargatePricingInfo fargatePricingInfo = vmPricingService.getFargatePricingInfo(region);

    Double cpuUnits = instanceData.getTotalResource().getCpuUnits();
    Double memoryMb = instanceData.getTotalResource().getMemoryMb();

    if (null != instanceData.getPricingResource()) {
      cpuUnits = instanceData.getPricingResource().getCpuUnits();
      memoryMb = instanceData.getPricingResource().getMemoryMb();
    }

    double cpuPricePerHour = K8sResourceUtils.getFargateVCpu(cpuUnits) * fargatePricingInfo.getCpuPrice();
    double memoryPricePerHour = K8sResourceUtils.getFargateMemoryGb(memoryMb) * fargatePricingInfo.getMemoryPrice();
    return PricingData.builder()
        .pricePerHour(cpuPricePerHour + memoryPricePerHour)
        .cpuUnit(instanceData.getTotalResource().getCpuUnits())
        .memoryMb(instanceData.getTotalResource().getMemoryMb())
        .build();
  }
}
