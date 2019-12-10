package io.harness.perpetualtask.k8s.watch;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.perpetualtask.k8s.watch.functions.PodOwnerMappingFunction;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.constraints.NotNull;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PodOwnerHelper {
  final Map<String, PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner>>
      ownerKindToNameFunctionMap;
  final Map<String, Map<String, Owner>> ownerKindToNameMap;

  @Inject
  PodOwnerHelper(Map<String, PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner>>
          ownerKindToNameFunctionMap) {
    this.ownerKindToNameFunctionMap = ownerKindToNameFunctionMap;
    this.ownerKindToNameMap = Maps.newConcurrentMap();
  }

  Owner getOwner(@NotNull Pod pod, KubernetesClient kubernetesClient) {
    return pod.getMetadata()
        .getOwnerReferences()
        .stream()
        .filter(or -> or != null)
        .findFirst()
        .map(parentOwnerReference -> {
          String parentOwnerKind = parentOwnerReference.getKind();
          String parentOwnerName = parentOwnerReference.getName();
          ownerKindToNameMap.putIfAbsent(parentOwnerKind, new ConcurrentHashMap<>());
          Map<String, Owner> ownerNameToWorkloadNameMap = ownerKindToNameMap.get(parentOwnerKind);
          Owner owner = ownerNameToWorkloadNameMap.get(parentOwnerName);
          if (owner == null) {
            logger.info("Fetching owner name and kind for parent owner of kind = {} & name = {}", parentOwnerKind,
                parentOwnerName);
            PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner> nameFunction =
                ownerKindToNameFunctionMap.get(parentOwnerKind);
            if (nameFunction != null) {
              owner = nameFunction.apply(parentOwnerReference, kubernetesClient, pod.getMetadata().getNamespace());
              ownerNameToWorkloadNameMap.put(parentOwnerName, owner);
            }
          }
          return owner;
        })
        .orElse(null);
  }
}
