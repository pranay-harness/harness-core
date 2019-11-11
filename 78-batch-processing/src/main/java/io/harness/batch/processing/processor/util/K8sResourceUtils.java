package io.harness.batch.processing.processor.util;

import io.harness.batch.processing.ccm.Resource;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class K8sResourceUtils {
  private static final String K8S_CPU_RESOURCE = "cpu";
  private static final String K8S_MEMORY_RESOURCE = "memory";

  public static Resource getResource(io.harness.perpetualtask.k8s.watch.Resource resource) {
    Quantity cpuQuantity = resource.getRequestsMap().get(K8S_CPU_RESOURCE);
    Quantity memQuantity = resource.getRequestsMap().get(K8S_MEMORY_RESOURCE);

    return Resource.builder()
        .cpuUnits(Double.valueOf(cpuQuantity.getAmount()))
        .memoryMb(Double.valueOf(memQuantity.getAmount()))
        .build();
  }
}
