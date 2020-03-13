package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.processor.util.K8sResourceUtils;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InstanceResourceServiceImpl implements InstanceResourceService {
  @Autowired private VMPricingService vmPricingService;

  private static final String GCP_CUSTOM_INSTANCE_PREFIX = "custom-";

  @Override
  public Resource getComputeVMResource(String instanceType, String region, CloudProvider cloudProvider) {
    double cpu, memory;
    if (instanceType.startsWith(GCP_CUSTOM_INSTANCE_PREFIX) && cloudProvider == CloudProvider.GCP) {
      String[] split = instanceType.split("-");
      cpu = Double.parseDouble(split[1]);
      memory = Double.parseDouble(split[2]) / 1024.0;
    } else {
      VMComputePricingInfo computeVMPricingInfo =
          vmPricingService.getComputeVMPricingInfo(instanceType, region, cloudProvider);
      cpu = computeVMPricingInfo.getCpusPerVm();
      memory = computeVMPricingInfo.getMemPerVm();
    }
    return K8sResourceUtils.getResource(cpu, memory);
  }
}
