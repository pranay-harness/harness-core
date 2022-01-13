/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import static java.util.Optional.ofNullable;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.watch.Quantity.Builder;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1PersistentVolumeSpec;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CE)
@UtilityClass
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class K8sResourceUtils {
  private static final String K8S_CPU_RESOURCE = "cpu";
  private static final String K8S_MEMORY_RESOURCE = "memory";
  private static final String K8S_STORAGE_RESOURCE = "storage";
  private static final String K8S_POD_RESOURCE = "pods";

  public static Resource getResource(V1Container k8sContainer) {
    // get the resource for each container
    Resource.Builder resourceBuilder = Resource.newBuilder()
                                           .putRequests(K8S_CPU_RESOURCE, getCpuRequest(k8sContainer))
                                           .putRequests(K8S_MEMORY_RESOURCE, getMemRequest(k8sContainer))
                                           .putLimits(K8S_CPU_RESOURCE, getCpuLimit(k8sContainer))
                                           .putLimits(K8S_MEMORY_RESOURCE, getMemLimit(k8sContainer));
    return resourceBuilder.build();
  }

  private static Quantity getCpuRequest(V1Container k8sContainer) {
    Builder cpuRequestBuilder = Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return cpuRequestBuilder.build();
    }

    Map<String, io.kubernetes.client.custom.Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_CPU_RESOURCE) != null) {
      cpuRequestBuilder.setAmount(K8sResourceStandardizer.getCpuNano(resourceRequestsMap.get(K8S_CPU_RESOURCE)));
      cpuRequestBuilder.setUnit("n");
    }
    return cpuRequestBuilder.build();
  }

  private static Quantity getMemRequest(V1Container k8sContainer) {
    Builder memRequestBuilder = Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return memRequestBuilder.build();
    }

    Map<String, io.kubernetes.client.custom.Quantity> resourceRequestsMap = k8sContainer.getResources().getRequests();
    if (resourceRequestsMap != null && resourceRequestsMap.get(K8S_MEMORY_RESOURCE) != null) {
      memRequestBuilder.setAmount(K8sResourceStandardizer.getMemoryByte(resourceRequestsMap.get(K8S_MEMORY_RESOURCE)));
      memRequestBuilder.setUnit("");
    }
    return memRequestBuilder.build();
  }

  private static Quantity getCpuLimit(V1Container k8sContainer) {
    Builder cpuLimitBuilder = Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return cpuLimitBuilder.build();
    }

    Map<String, io.kubernetes.client.custom.Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
    if (resourceLimitsMap != null && resourceLimitsMap.get(K8S_CPU_RESOURCE) != null) {
      cpuLimitBuilder.setAmount(K8sResourceStandardizer.getCpuNano(resourceLimitsMap.get(K8S_CPU_RESOURCE)));
      cpuLimitBuilder.setUnit("n");
    }
    return cpuLimitBuilder.build();
  }

  private static Quantity getMemLimit(V1Container k8sContainer) {
    Builder memLimitBuilder = Quantity.newBuilder();
    if (k8sContainer.getResources() == null) {
      return memLimitBuilder.build();
    }

    Map<String, io.kubernetes.client.custom.Quantity> resourceLimitsMap = k8sContainer.getResources().getLimits();
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
      Map<String, io.kubernetes.client.custom.Quantity> resourceRequests = container.getResources().getRequests();
      reqCpuNanos += getCpuNanos(resourceRequests);
      reqMemBytes += getMemBytes(resourceRequests);
      Map<String, io.kubernetes.client.custom.Quantity> resourceLimits = container.getResources().getLimits();
      limCpuNanos += getCpuNanos(resourceLimits);
      limMemBytes += getMemBytes(resourceLimits);
    }

    if (podSpec.getInitContainers() != null) {
      for (V1Container initContainer : podSpec.getInitContainers()) {
        Map<String, io.kubernetes.client.custom.Quantity> resourceRequests = initContainer.getResources().getRequests();
        reqCpuNanos = Math.max(reqCpuNanos, getCpuNanos(resourceRequests));
        reqMemBytes = Math.max(reqMemBytes, getMemBytes(resourceRequests));
        Map<String, io.kubernetes.client.custom.Quantity> resourceLimits = initContainer.getResources().getLimits();
        limCpuNanos = Math.max(limCpuNanos, getCpuNanos(resourceLimits));
        limMemBytes = Math.max(limMemBytes, getMemBytes(resourceLimits));
      }
    }

    return Resource.newBuilder()
        .putRequests(K8S_CPU_RESOURCE, Quantity.newBuilder().setAmount(reqCpuNanos).setUnit("n").build())
        .putRequests(K8S_MEMORY_RESOURCE, Quantity.newBuilder().setAmount(reqMemBytes).setUnit("").build())
        .putLimits(K8S_CPU_RESOURCE, Quantity.newBuilder().setAmount(limCpuNanos).setUnit("n").build())
        .putLimits(K8S_MEMORY_RESOURCE, Quantity.newBuilder().setAmount(limMemBytes).setUnit("").build())
        .build();
  }

  static Map<String, Quantity> getResourceMap(Map<String, io.kubernetes.client.custom.Quantity> resourceMap) {
    long cpuNanos = getCpuNanos(resourceMap);
    long memBytes = getMemBytes(resourceMap);
    long pods = getPods(resourceMap);
    return ImmutableMap.<String, Quantity>builder()
        .put(K8S_CPU_RESOURCE, Quantity.newBuilder().setUnit("n").setAmount(cpuNanos).build())
        .put(K8S_MEMORY_RESOURCE, Quantity.newBuilder().setUnit("").setAmount(memBytes).build())
        .put(K8S_POD_RESOURCE, Quantity.newBuilder().setAmount(pods).setUnit("").build())
        .build();
  }

  private static long getPods(Map<String, io.kubernetes.client.custom.Quantity> resourceMap) {
    return ofNullable(resourceMap)
        .map(resReq -> resReq.get(K8S_POD_RESOURCE))
        .filter(Objects::nonNull)
        .map(q -> q.getNumber().longValue())
        .orElse(0L);
  }

  private static long getMemBytes(Map<String, io.kubernetes.client.custom.Quantity> resourceMap) {
    return ofNullable(resourceMap)
        .map(resReq -> resReq.get(K8S_MEMORY_RESOURCE))
        .map(K8sResourceStandardizer::getMemoryByte)
        .orElse(0L);
  }

  private static long getCpuNanos(Map<String, io.kubernetes.client.custom.Quantity> resourceMap) {
    return ofNullable(resourceMap)
        .map(resMap -> resMap.get(K8S_CPU_RESOURCE))
        .map(K8sResourceStandardizer::getCpuNano)
        .orElse(0L);
  }

  public static Quantity getStorageRequest(V1ResourceRequirements resources) {
    Builder storageRequestBuilder = Quantity.newBuilder();
    if (resources != null && resources.getRequests() != null) {
      storageRequestBuilder.setAmount(
          ofNullable(resources.getRequests().get(K8S_STORAGE_RESOURCE)).map(x -> x.getNumber().longValue()).orElse(0L));
      return storageRequestBuilder.setUnit("B").build();
    }
    log.warn("Returning default storge value");
    // no unit when the value is default.
    return storageRequestBuilder.setAmount(0L).build();
  }

  public static Quantity getStorageCapacity(V1PersistentVolumeSpec spec) {
    Builder storageRequestBuilder = Quantity.newBuilder();
    if (spec != null && spec.getCapacity() != null) {
      storageRequestBuilder.setAmount(
          ofNullable(spec.getCapacity().get(K8S_STORAGE_RESOURCE)).map(x -> x.getNumber().longValue()).orElse(0L));
      return storageRequestBuilder.setUnit("B").build();
    }
    log.warn("Returning default storge value");
    // no unit when the value is default.
    return storageRequestBuilder.setAmount(0L).build();
  }
}
