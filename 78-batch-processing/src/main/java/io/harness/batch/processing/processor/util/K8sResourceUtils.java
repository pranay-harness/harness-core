package io.harness.batch.processing.processor.util;

import io.harness.batch.processing.ccm.Resource;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@UtilityClass
@Slf4j
public class K8sResourceUtils {
  private static final String K8S_CPU_RESOURCE = "cpu";
  private static final String K8S_MEMORY_RESOURCE = "memory";
  private static final double NANO_TO_UNIT = 1_000_000_000L;
  private static final double UNIT_TO_MEBI = 1 << 20;

  public static Resource getResource(Map<String, Quantity> resource) {
    Quantity cpuQuantity = resource.get(K8S_CPU_RESOURCE);
    Quantity memQuantity = resource.get(K8S_MEMORY_RESOURCE);

    return Resource.builder()
        .cpuUnits(cpuQuantity.getAmount() / NANO_TO_UNIT)
        .memoryMb(memQuantity.getAmount() / UNIT_TO_MEBI)
        .build();
  }
}
