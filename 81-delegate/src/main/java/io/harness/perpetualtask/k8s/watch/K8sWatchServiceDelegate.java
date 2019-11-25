package io.harness.perpetualtask.k8s.watch;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.serializer.KryoUtils;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class K8sWatchServiceDelegate {
  private static final String RESOURCE_KIND_POD = "Pod";
  private static final String RESOURCE_KIND_NODE = "Node";
  private static final Set<String> WATCHABLE = ImmutableSet.of(RESOURCE_KIND_POD, RESOURCE_KIND_NODE);

  private final WatcherFactory watcherFactory;
  private final KubernetesClientFactory kubernetesClientFactory;

  private final Map<String, Watcher> watchMap; // <id, Watch>

  @Inject
  public K8sWatchServiceDelegate(WatcherFactory watcherFactory, KubernetesClientFactory kubernetesClientFactory) {
    this.watcherFactory = watcherFactory;
    this.kubernetesClientFactory = kubernetesClientFactory;
    this.watchMap = new ConcurrentHashMap<>();
  }

  public Iterable<String> watchIds() {
    return watchMap.keySet();
  }

  public String create(K8sWatchTaskParams params) {
    String resourceKind = params.getK8SResourceKind();
    checkArgument(WATCHABLE.contains(resourceKind), "Resource kind %s is not watchable", resourceKind);
    String watchId = makeWatchId(params);
    // computeIfAbsent form required for lookup & create to be atomic.
    watchMap.computeIfAbsent(watchId, id -> {
      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) KryoUtils.asObject(params.getK8SClusterConfig().toByteArray());
      KubernetesClient client = kubernetesClientFactory.newKubernetesClient(k8sClusterConfig);
      Watcher watcher;
      switch (resourceKind) {
        case RESOURCE_KIND_POD:
          watcher = watcherFactory.createPodWatcher(client, params);
          break;
        case RESOURCE_KIND_NODE:
          watcher = watcherFactory.createNodeWatcher(client, params);
          break;
        default:
          // Unreachable (checked in precondition)
          throw new IllegalArgumentException("Unwatchable resource kind");
      }
      return watcher;
    });

    return watchId;
  }

  public void delete(String watchId) {
    // computeIfPresent form required for lookup & delete to be atomic.
    watchMap.computeIfPresent(watchId, (id, watcher) -> {
      watcher.onClose(null);
      return null;
    });
  }

  private String makeWatchId(K8sWatchTaskParams params) {
    // Deduplicate requests for watching same kind of resource on the same cloud provider.
    return Joiner.on("_").join(params.getCloudProviderId(), params.getK8SResourceKind());
  }
}
