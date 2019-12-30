package io.harness.perpetualtask.k8s.watch;

import static java.util.Objects.requireNonNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@UtilityClass
@Slf4j
public class K8sWorkloadUtils {
  private static final Cache<CacheKey, HasMetadata> resourceCache =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

  @Value
  private static class CacheKey {
    private String masterUrl;
    private String namespace;
    private String kind;
    private String name;
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
      final CacheKey cacheKey = new CacheKey(client.getMasterUrl().toString(), resource.getMetadata().getNamespace(),
          ownerReference.getKind(), ownerReference.getName());
      Function<CacheKey, HasMetadata> ownerMapper = key -> null;
      switch (ownerKind) {
        case "Deployment":
          ownerMapper = key -> client.extensions().deployments().inNamespace(key.namespace).withName(key.name).get();
          break;
        case "DaemonSet":
          ownerMapper = key -> client.extensions().daemonSets().inNamespace(key.namespace).withName(key.name).get();
          break;
        case "StatefulSet":
          ownerMapper = key -> client.apps().statefulSets().inNamespace(key.namespace).withName(key.name).get();
          break;
        case "ReplicaSet":
          ownerMapper = key -> client.extensions().replicaSets().inNamespace(key.namespace).withName(key.name).get();
          break;
        case "Job":
          ownerMapper = key -> client.extensions().jobs().inNamespace(key.namespace).withName(key.name).get();
          break;
        case "CronJob":
          // supporting CronJob requires fabric8 client version upgrade
          // fallthrough
        default:
          logger.warn("Unsupported owner: {}", ownerReference);
          break;
      }
      HasMetadata newResource = resourceCache.get(cacheKey, ownerMapper);
      if (newResource == null) {
        break;
      }
      resource = newResource;
    }
    return resource;
  }

  public Owner getTopLevelOwner(KubernetesClient client, Pod pod) {
    HasMetadata topLevelOwner = getTopLevelController(client, pod);
    return Owner.newBuilder()
        .setKind(topLevelOwner.getKind())
        .setName(topLevelOwner.getMetadata().getName())
        .setUid(topLevelOwner.getMetadata().getUid())
        .build();
  }
}
