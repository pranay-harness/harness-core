package io.harness.batch.processing.tasklet.util;

import static java.util.Optional.ofNullable;

import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.beans.StorageResource;
import io.harness.perpetualtask.k8s.watch.Quantity;

import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class K8sResourceUtils {
  private static final String K8S_CPU_RESOURCE = "cpu";
  private static final String K8S_MEMORY_RESOURCE = "memory";
  private static final String K8S_POD_RESOURCE = "pods";

  private static final double NANO_TO_UNIT = 1_000_000_000L;
  private static final double UNIT_TO_MEBI = 1 << 20;
  private static final double CPU_UNITS = 1024;
  private static final double GBI_TO_MBI = 1024;

  public static Resource getResource(Map<String, Quantity> resource) {
    Quantity cpuQuantity = resource.get(K8S_CPU_RESOURCE);
    Quantity memQuantity = resource.get(K8S_MEMORY_RESOURCE);

    return Resource.builder()
        .cpuUnits(getCpuUnits(cpuQuantity.getAmount()))
        .memoryMb(getMemoryMb(memQuantity.getAmount()))
        .build();
  }

  public static double getCpuUnits(double nanoCpu) {
    return (nanoCpu / NANO_TO_UNIT) * CPU_UNITS;
  }

  public static double getMemoryMb(double memoryBytes) {
    return memoryBytes / UNIT_TO_MEBI;
  }

  public static Resource getResource(double vCPU, double memoryGB) {
    return Resource.builder().cpuUnits(vCPU * CPU_UNITS).memoryMb(memoryGB * GBI_TO_MBI).build();
  }

  public static StorageResource getCapacity(Quantity capacity) {
    return StorageResource.builder().capacity(getMemoryMb(capacity.getAmount())).build();
  }

  public static long getPodCapacity(Map<String, Quantity> resource) {
    return ofNullable(resource.get(K8S_POD_RESOURCE)).map(Quantity::getAmount).orElse(0L);
  }
}
