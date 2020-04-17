package io.harness.perpetualtask.k8s.watch;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity.Builder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@UtilityClass
@Slf4j
public class K8sResourceUtils {
  private static final String K8S_CPU_RESOURCE = "cpu";
  private static final String K8S_MEMORY_RESOURCE = "memory";

  public static Resource getResource(Container k8sContainer) {
    // get the resource for each container
    Resource.Builder resourceBuilder = Resource.newBuilder()
                                           .putRequests(K8S_CPU_RESOURCE, getCpuRequest(k8sContainer))
                                           .putRequests(K8S_MEMORY_RESOURCE, getMemRequest(k8sContainer))
                                           .putLimits(K8S_CPU_RESOURCE, getCpuLimit(k8sContainer))
                                           .putLimits(K8S_MEMORY_RESOURCE, getMemLimit(k8sContainer));
    return resourceBuilder.build();
  }

  private static Resource.Quantity getCpuRequest(Container k8sContainer) {
    Builder cpuRequestBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return cpuRequestBuilder.build();
    }

    Map<String, Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_CPU_RESOURCE) != null) {
      String cpuRequestAmount = resourceRequestsMap.get(K8S_CPU_RESOURCE).getAmount();
      if (!StringUtils.isBlank(cpuRequestAmount)) {
        cpuRequestBuilder.setAmount(K8sResourceStandardizer.getCpuNano(cpuRequestAmount));
        cpuRequestBuilder.setUnit("n");
      }
    }
    return cpuRequestBuilder.build();
  }

  private static Resource.Quantity getMemRequest(Container k8sContainer) {
    Builder memRequestBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return memRequestBuilder.build();
    }

    Map<String, Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_MEMORY_RESOURCE) != null) {
      String memRequestAmount = resourceRequestsMap.get(K8S_MEMORY_RESOURCE).getAmount();
      if (!StringUtils.isBlank(memRequestAmount)) {
        memRequestBuilder.setAmount(K8sResourceStandardizer.getMemoryByte(memRequestAmount));
        memRequestBuilder.setUnit("");
      }
    }
    return memRequestBuilder.build();
  }

  private static Resource.Quantity getCpuLimit(Container k8sContainer) {
    Builder cpuLimitBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return cpuLimitBuilder.build();
    }

    Map<String, Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_CPU_RESOURCE) != null) {
      String cpuLimitAmount = resourceLimitsMap.get(K8S_CPU_RESOURCE).getAmount();
      if (!StringUtils.isBlank(cpuLimitAmount)) {
        cpuLimitBuilder.setAmount(K8sResourceStandardizer.getCpuNano(cpuLimitAmount));
        cpuLimitBuilder.setUnit("n");
      }
    }
    return cpuLimitBuilder.build();
  }

  private static Resource.Quantity getMemLimit(Container k8sContainer) {
    Builder memLimitBuilder = Resource.Quantity.newBuilder();
    if (isNull(k8sContainer.getResources())) {
      logger.warn("This k8s container is missing resource.");
      return memLimitBuilder.build();
    }

    Map<String, Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_MEMORY_RESOURCE) != null) {
      String memLimitAmount = resourceLimitsMap.get(K8S_MEMORY_RESOURCE).getAmount();
      if (!StringUtils.isBlank(memLimitAmount)) {
        memLimitBuilder.setAmount(K8sResourceStandardizer.getMemoryByte(memLimitAmount));
        memLimitBuilder.setUnit("");
      }
    }
    return memLimitBuilder.build();
  }

  static Resource getTotalResourceRequest(PodSpec podSpec) {
    long cpuNanos = 0;
    long memBytes = 0;
    for (Container container : podSpec.getContainers()) {
      Map<String, Quantity> resourceRequests = container.getResources().getRequests();
      cpuNanos += getCpuNanos(resourceRequests);
      memBytes += getMemBytes(resourceRequests);
    }

    for (Container initContainer : podSpec.getInitContainers()) {
      Map<String, Quantity> resourceRequests = initContainer.getResources().getRequests();
      cpuNanos = Math.max(cpuNanos, getCpuNanos(resourceRequests));
      memBytes = Math.max(memBytes, getMemBytes(resourceRequests));
    }
    return Resource.newBuilder()
        .putRequests(K8S_CPU_RESOURCE, Resource.Quantity.newBuilder().setAmount(cpuNanos).setUnit("n").build())
        .putRequests(K8S_MEMORY_RESOURCE, Resource.Quantity.newBuilder().setAmount(memBytes).setUnit("").build())
        .build();
  }

  public static Map<String, Resource.Quantity> getResourceMap(Map<String, Quantity> resourceMap) {
    long cpuNanos = getCpuNanos(resourceMap);
    long memBytes = getMemBytes(resourceMap);
    return ImmutableMap.<String, Resource.Quantity>builder()
        .put(K8S_CPU_RESOURCE, Resource.Quantity.newBuilder().setUnit("n").setAmount(cpuNanos).build())
        .put(K8S_MEMORY_RESOURCE, Resource.Quantity.newBuilder().setUnit("").setAmount(memBytes).build())
        .build();
  }

  private static long getMemBytes(Map<String, Quantity> resourceMap) {
    return ofNullable(resourceMap)
        .map(resReq -> resReq.get(K8S_MEMORY_RESOURCE))
        .map(Quantity::getAmount)
        .map(K8sResourceStandardizer::getMemoryByte)
        .orElse(0L);
  }

  private static long getCpuNanos(Map<String, Quantity> resourceMap) {
    return ofNullable(resourceMap)
        .map(resMap -> resMap.get(K8S_CPU_RESOURCE))
        .map(Quantity::getAmount)
        .map(K8sResourceStandardizer::getCpuNano)
        .orElse(0L);
  }
}
