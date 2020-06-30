package io.harness.perpetualtask.k8s.watch;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import io.kubernetes.client.informer.cache.Store;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.ObjectAccessor;
import io.kubernetes.client.util.exception.ObjectMetaReflectException;
import lombok.Value;

import java.util.Map;

public class K8sControllerFetcher {
  private final Map<String, Store<?>> stores;

  public K8sControllerFetcher(Map<String, Store<?>> stores) {
    this.stores = stores;
  }

  private Workload getTopLevelController(Workload workload) {
    while (hasController(workload)) {
      final V1OwnerReference ownerReference = requireNonNull(getController(workload));
      Workload ownerResource = getWorkload(workload.getObjectMeta().getNamespace(), ownerReference);
      if (ownerResource == null) {
        break;
      }
      workload = ownerResource;
    }
    return workload;
  }

  private boolean hasController(Workload resource) {
    return getController(resource) != null;
  }

  private V1OwnerReference getController(Workload workload) {
    if (workload != null) {
      return ofNullable(workload.getObjectMeta().getOwnerReferences())
          .orElse(emptyList())
          .stream()
          .filter(or -> Boolean.TRUE.equals(or.getController())) // only want controller
          .findAny() // only 1 controller possible
          .orElse(null);
    }
    return null;
  }

  private Workload getWorkload(String namespace, V1OwnerReference ownerReference) {
    Store<?> store = stores.get(ownerReference.getKind());
    if (store != null) {
      Object workload = store.getByKey(namespace + "/" + ownerReference.getName());
      return Workload.of(ownerReference.getKind(), getObjectMeta(workload));
    }
    // This indicates it is not one of the well-known workloads.
    return Workload.of(ownerReference.getKind(),
        new V1ObjectMetaBuilder()
            .withName(ownerReference.getName())
            .withNamespace(namespace)
            .withUid(ownerReference.getUid())
            .build());
  }

  private V1ObjectMeta getObjectMeta(Object workloadResource) {
    V1ObjectMeta objectMeta;
    try {
      objectMeta = ObjectAccessor.objectMetadata(workloadResource);
    } catch (ObjectMetaReflectException e) {
      throw new IllegalArgumentException("Workload should have metadata", e);
    }
    return objectMeta;
  }

  public Owner getTopLevelOwner(V1Pod pod) {
    Workload topLevelOwner = getTopLevelController(Workload.of("Pod", pod.getMetadata()));
    return Owner.newBuilder()
        .setKind(topLevelOwner.getKind())
        .setName(topLevelOwner.getObjectMeta().getName())
        .setUid(topLevelOwner.getObjectMeta().getUid())
        .putAllLabels(ofNullable(topLevelOwner.getObjectMeta().getLabels()).orElse(emptyMap()))
        .build();
  }

  @Value(staticConstructor = "of")
  private static class Workload {
    String kind;
    V1ObjectMeta objectMeta;
  }
}
