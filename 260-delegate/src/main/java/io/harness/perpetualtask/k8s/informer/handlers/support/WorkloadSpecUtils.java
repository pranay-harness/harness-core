/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.k8s.informer.handlers.support;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class WorkloadSpecUtils {
  private K8sWorkloadSpec.ContainerSpec makeContainerSpec(V1Container container) {
    K8sWorkloadSpec.ContainerSpec.Builder builder = K8sWorkloadSpec.ContainerSpec.newBuilder();
    builder.setName(container.getName());
    V1ResourceRequirements resources =
        Optional.ofNullable(container.getResources()).orElseGet(V1ResourceRequirements::new);
    builder.putAllRequests(convertQuantityToString(resources.getRequests()));
    builder.putAllLimits(convertQuantityToString(resources.getLimits()));
    return builder.build();
  }

  private static Map<String, String> convertQuantityToString(@Nullable Map<String, Quantity> resourceMap) {
    return Optional.ofNullable(resourceMap)
        .orElse(Collections.emptyMap())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toSuffixedString()));
  }

  public static List<K8sWorkloadSpec.ContainerSpec> makeContainerSpecs(@Nullable List<V1Container> containers) {
    return Optional.ofNullable(containers)
        .orElse(Collections.emptyList())
        .stream()
        .map(WorkloadSpecUtils::makeContainerSpec)
        .collect(Collectors.toList());
  }
}
