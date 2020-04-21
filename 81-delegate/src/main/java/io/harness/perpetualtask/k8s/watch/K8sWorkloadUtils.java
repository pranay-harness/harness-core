package io.harness.perpetualtask.k8s.watch;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.perpetualtask.k8s.cronjobs.client.K8sCronJobClient;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@UtilityClass
@Slf4j
class K8sWorkloadUtils {
  private static final Cache<CacheKey, HasMetadata> resourceCache =
      Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

  @Value
  private static class CacheKey {
    String masterUrl;
    String namespace;
    String kind;
    String name;
  }

  private boolean hasController(HasMetadata resource) {
    return getController(resource) != null;
  }

  private OwnerReference getController(HasMetadata resource) {
    if (resource.getMetadata() != null) {
      return resource.getMetadata()
          .getOwnerReferences()
          .stream()
          .filter(or -> Boolean.TRUE.equals(or.getController())) // only want controller
          .findAny() // only 1 controller possible
          .orElse(null);
    }
    return null;
  }

  // Follow ownerReferences recursively until we reach top level controller
  private HasMetadata getTopLevelController(KubernetesClient client, HasMetadata resource) {
    while (hasController(resource)) {
      final OwnerReference ownerReference = requireNonNull(getController(resource));
      final String ownerKind = ownerReference.getKind();
      HasMetadata ownerResource =
          getWorkload(client, resource.getMetadata().getNamespace(), ownerKind, ownerReference.getName());
      if (ownerResource == null) {
        break;
      }
      resource = ownerResource;
    }
    return resource;
  }

  private HasMetadata getWorkload(KubernetesClient client, String namespace, String kind, String name) {
    final CacheKey cacheKey = new CacheKey(client.getMasterUrl().toString(), namespace, kind, name);
    return resourceCache.get(cacheKey, key -> {
      switch (key.kind) {
        case "Deployment":
          return client.extensions().deployments().inNamespace(key.namespace).withName(key.name).get();
        case "DaemonSet":
          return client.extensions().daemonSets().inNamespace(key.namespace).withName(key.name).get();
        case "StatefulSet":
          return client.apps().statefulSets().inNamespace(key.namespace).withName(key.name).get();
        case "ReplicaSet":
          return client.extensions().replicaSets().inNamespace(key.namespace).withName(key.name).get();
        case "Job":
          return client.extensions().jobs().inNamespace(key.namespace).withName(key.name).get();
        case "CronJob":
          return client.adapt(K8sCronJobClient.class).cronJobs().inNamespace(key.namespace).withName(key.name).get();
        case "Pod":
          return client.pods().inNamespace(key.namespace).withName(key.name).get();
        default:
          return UnknownResource.builder()
              .kind(kind)
              // TODO(avmohan) - query for uid with dynamic query after migrating to k8s java client
              .metadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).withUid("").build())
              .build();
      }
    });
  }

  Owner getTopLevelOwner(KubernetesClient client, Pod pod) {
    HasMetadata topLevelOwner = getTopLevelController(client, pod);
    return Owner.newBuilder()
        .setKind(topLevelOwner.getKind())
        .setName(topLevelOwner.getMetadata().getName())
        .setUid(topLevelOwner.getMetadata().getUid())
        .putAllLabels(ofNullable(topLevelOwner.getMetadata().getLabels()).orElse(emptyMap()))
        .build();
  }

  @Data
  @Builder
  static class UnknownResource implements HasMetadata {
    ObjectMeta metadata;
    String kind;
    String apiVersion;
  }
}
