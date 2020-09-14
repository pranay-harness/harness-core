package io.harness.batch.processing.pricing.service.support;

import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.pricing.data.CloudProvider;
import org.springframework.stereotype.Component;

@Component
public class GCPCustomInstanceDetailProvider {
  private static final String GCP_CUSTOM_INSTANCE_PREFIX = "custom-";

  private GCPCustomInstanceDetailProvider() {}

  public static boolean isCustomGCPInstance(String instanceType, CloudProvider cloudProvider) {
    boolean gcpCustomInstance = false;
    if (cloudProvider == CloudProvider.GCP && instanceType.contains(GCP_CUSTOM_INSTANCE_PREFIX)) {
      gcpCustomInstance = true;
    }
    return gcpCustomInstance;
  }

  public static Resource getCustomGcpInstanceResource(String instanceType) {
    String[] split = instanceType.split("-");
    double cpu = Double.parseDouble(split[split.length - 2]);
    double memory = Double.parseDouble(split[split.length - 1]);
    return Resource.builder().cpuUnits(cpu * 1024.0).memoryMb(memory).build();
  }

  public static PricingData getGCPCustomInstancePricingData(String instanceFamily, InstanceCategory instanceCategory) {
    double cpuPricePerHr = 0.033174;
    double memoryPricePerHr = 0.004446;
    if (instanceCategory == InstanceCategory.SPOT) {
      cpuPricePerHr = 0.00698;
      memoryPricePerHr = 0.00094;
    }
    Resource resource = getCustomGcpInstanceResource(instanceFamily);
    double cpu = resource.getCpuUnits();
    double memory = resource.getMemoryMb();
    double pricePerHr = ((cpuPricePerHr / 1024) * cpu) + ((memoryPricePerHr / 1024) * memory);
    return PricingData.builder().pricePerHour(pricePerHr).cpuUnit(cpu).memoryMb(memory).build();
  }
}
