package io.harness.perpetualtask.k8s.watch;

import static java.util.Optional.ofNullable;

import com.google.common.collect.ImmutableMap;

import io.harness.perpetualtask.k8s.watch.Resource.Quantity.Builder;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1PodSpec;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@UtilityClass
@Slf4j
public class K8sResourceUtils {
  private static final String K8S_CPU_RESOURCE = "cpu";
  private static final String K8S_MEMORY_RESOURCE = "memory";

  public static Resource getResource(V1Container k8sContainer) {
    // get the resource for each container
    Resource.Builder resourceBuilder = Resource.newBuilder()
                                           .putRequests(K8S_CPU_RESOURCE, getCpuRequest(k8sContainer))
                                           .putRequests(K8S_MEMORY_RESOURCE, getMemRequest(k8sContainer))
                                           .putLimits(K8S_CPU_RESOURCE, getCpuLimit(k8sContainer))
                                           .putLimits(K8S_MEMORY_RESOURCE, getMemLimit(k8sContainer));
    return resourceBuilder.build();
  }

  private static Resource.Quantity getCpuRequest(V1Container k8sContainer) {
    Builder cpuRequestBuilder = Resource.Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return cpuRequestBuilder.build();
    }

    Map<String, Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_CPU_RESOURCE) != null) {
      cpuRequestBuilder.setAmount(K8sResourceStandardizer.getCpuNano(resourceRequestsMap.get(K8S_CPU_RESOURCE)));
      cpuRequestBuilder.setUnit("n");
    }
    return cpuRequestBuilder.build();
  }

  private static Resource.Quantity getMemRequest(V1Container k8sContainer) {
    Builder memRequestBuilder = Resource.Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return memRequestBuilder.build();
    }

    Map<String, Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_MEMORY_RESOURCE) != null) {
      memRequestBuilder.setAmount(K8sResourceStandardizer.getMemoryByte(resourceRequestsMap.get(K8S_MEMORY_RESOURCE)));
      memRequestBuilder.setUnit("");
    }
    return memRequestBuilder.build();
  }

  private static Resource.Quantity getCpuLimit(V1Container k8sContainer) {
    Builder cpuLimitBuilder = Resource.Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return cpuLimitBuilder.build();
    }

    Map<String, Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_CPU_RESOURCE) != null) {
      cpuLimitBuilder.setAmount(K8sResourceStandardizer.getCpuNano(resourceLimitsMap.get(K8S_CPU_RESOURCE)));
      cpuLimitBuilder.setUnit("n");
    }
    return cpuLimitBuilder.build();
  }

  private static Resource.Quantity getMemLimit(V1Container k8sContainer) {
    Builder memLimitBuilder = Resource.Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return memLimitBuilder.build();
    }

    Map<String, Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_MEMORY_RESOURCE) != null) {
      memLimitBuilder.setAmount(K8sResourceStandardizer.getMemoryByte(resourceLimitsMap.get(K8S_MEMORY_RESOURCE)));
      memLimitBuilder.setUnit("");
    }
    return memLimitBuilder.build();
  }

  static Resource getEffectiveResources(V1PodSpec podSpec) {
    long reqCpuNanos = 0;
    long reqMemBytes = 0;
    long limCpuNanos = 0;
    long limMemBytes = 0;
    for (V1Container container : podSpec.getContainers()) {
      Map<String, Quantity> resourceRequests = container.getResources().getRequests();
      reqCpuNanos += getCpuNanos(resourceRequests);
      reqMemBytes += getMemBytes(resourceRequests);
      Map<String, Quantity> resourceLimits = container.getResources().getLimits();
      limCpuNanos += getCpuNanos(resourceLimits);
      limMemBytes += getMemBytes(resourceLimits);
    }

    if (podSpec.getInitContainers() != null) {
      for (V1Container initContainer : podSpec.getInitContainers()) {
        Map<String, Quantity> resourceRequests = initContainer.getResources().getRequests();
        reqCpuNanos = Math.max(reqCpuNanos, getCpuNanos(resourceRequests));
        reqMemBytes = Math.max(reqMemBytes, getMemBytes(resourceRequests));
        Map<String, Quantity> resourceLimits = initContainer.getResources().getLimits();
        limCpuNanos = Math.max(limCpuNanos, getCpuNanos(resourceLimits));
        limMemBytes = Math.max(limMemBytes, getMemBytes(resourceLimits));
      }
    }

    return Resource.newBuilder()
        .putRequests(K8S_CPU_RESOURCE, Resource.Quantity.newBuilder().setAmount(reqCpuNanos).setUnit("n").build())
        .putRequests(K8S_MEMORY_RESOURCE, Resource.Quantity.newBuilder().setAmount(reqMemBytes).setUnit("").build())
        .putLimits(K8S_CPU_RESOURCE, Resource.Quantity.newBuilder().setAmount(limCpuNanos).setUnit("n").build())
        .putLimits(K8S_MEMORY_RESOURCE, Resource.Quantity.newBuilder().setAmount(limMemBytes).setUnit("").build())
        .build();
  }

  static Map<String, Resource.Quantity> getResourceMap(Map<String, Quantity> resourceMap) {
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
        .map(K8sResourceStandardizer::getMemoryByte)
        .orElse(0L);
  }

  private static long getCpuNanos(Map<String, Quantity> resourceMap) {
    return ofNullable(resourceMap)
        .map(resMap -> resMap.get(K8S_CPU_RESOURCE))
        .map(K8sResourceStandardizer::getCpuNano)
        .orElse(0L);
  }
}
