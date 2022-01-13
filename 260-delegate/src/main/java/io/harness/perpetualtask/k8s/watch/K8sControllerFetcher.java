/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.common.collect.ImmutableSet;
import io.kubernetes.client.informer.cache.Store;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.ObjectAccessor;
import io.kubernetes.client.util.TypeAccessor;
import io.kubernetes.client.util.exception.ObjectMetaReflectException;
import io.kubernetes.client.util.exception.TypeMetaReflectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CE)
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
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
        return Workload.of(ownerReference.getKind(), getObjectMeta(workload), getWorkloadReplicas(workload));
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

  @SuppressWarnings("PMD")
  private Integer getWorkloadReplicas(@NotNull Object workloadResource) {
    // Deployment, ReplicaSet, StatefulSet have replicas field
    // DaemonSet, Job, CronJob doesn't have replicas
    Integer replicas = null;
    try {
      Method getSpecMethod = workloadResource.getClass().getMethod("getSpec");
      @Nullable Object specObject = getSpecMethod.invoke(workloadResource);

      Method getReplicasMethod = specObject.getClass().getMethod("getReplicas");
      replicas = (Integer) getReplicasMethod.invoke(specObject);
    } catch (NullPointerException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      try {
        String kind = TypeAccessor.kind(workloadResource);
        // if we get an error for workloads under consideration, throw error.
        if (ImmutableSet.of("Deployment", "ReplicaSet", "StatefulSet").contains(kind)) {
          replicas = 1;
          log.warn("Can't get replicas from workload specs of Kind:{}, defaulting to 1", kind, e);
        }
      } catch (TypeMetaReflectException typeMetaReflectException) {
        log.error("Exception while fetching workload type, not expected", typeMetaReflectException);
      }
    }
    return replicas;
  }

  public Owner getTopLevelOwner(V1Pod pod) {
    Workload topLevelOwner = getTopLevelController(Workload.of("Pod", pod.getMetadata(), 1));
    return Owner.newBuilder()
        .setKind(topLevelOwner.getKind())
        .setName(topLevelOwner.getObjectMeta().getName())
        .setUid(topLevelOwner.getObjectMeta().getUid())
        .putAllLabels(ofNullable(topLevelOwner.getObjectMeta().getLabels()).orElse(emptyMap()))
        .setReplicas(ofNullable(topLevelOwner.getReplicas()).orElse(0))
        .build();
  }
}
