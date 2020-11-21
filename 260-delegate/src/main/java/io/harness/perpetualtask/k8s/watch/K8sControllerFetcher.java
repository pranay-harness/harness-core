package io.harness.perpetualtask.k8s.watch;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import io.kubernetes.client.informer.cache.Store;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.ObjectAccessor;
import io.kubernetes.client.util.exception.ObjectMetaReflectException;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sControllerFetcher {
  private final Map<String, Store<?>> stores;
  private final CrdWorkloadFetcher crdWorkloadFetcher;

  public K8sControllerFetcher(Map<String, Store<?>> stores, CrdWorkloadFetcher crdWorkloadFetcher) {
    this.stores = stores;
    this.crdWorkloadFetcher = crdWorkloadFetcher;
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
      if (workload != null) {
        return Workload.of(ownerReference.getKind(), getObjectMeta(workload));
      }
    }

    // This indicates it is not one of the well-known workloads.
    // So delegate to the CrdWorkloadFetcher.
    return crdWorkloadFetcher.getWorkload(CrdWorkloadFetcher.WorkloadReference.builder()
                                              .namespace(namespace)
                                              .name(ownerReference.getName())
                                              .kind(ownerReference.getKind())
                                              .apiVersion(ownerReference.getApiVersion())
                                              .uid(ownerReference.getUid())
                                              .build());
  }

  private V1ObjectMeta getObjectMeta(@NotNull Object workloadResource) {
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
}
