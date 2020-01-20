package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class K8sWatchServiceDelegate {
  private final WatcherFactory watcherFactory;
  private final KubernetesClientFactory kubernetesClientFactory;

  private final Map<String, NodePodWatcher> watchMap; // <id, Watch>

  @Inject
  public K8sWatchServiceDelegate(WatcherFactory watcherFactory, KubernetesClientFactory kubernetesClientFactory) {
    this.watcherFactory = watcherFactory;
    this.kubernetesClientFactory = kubernetesClientFactory;
    this.watchMap = new ConcurrentHashMap<>();
  }

  @Builder
  static class NodePodWatcher {
    private Watcher nodeWatcher;
    private Watcher podWatcher;
  }

  public Iterable<String> watchIds() {
    return watchMap.keySet();
  }

  public String create(K8sWatchTaskParams params) {
    String watchId = params.getClusterId();
    // computeIfAbsent form required for lookup & create to be atomic.
    watchMap.computeIfAbsent(watchId, id -> {
      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) KryoUtils.asObject(params.getK8SClusterConfig().toByteArray());
      KubernetesClient client = kubernetesClientFactory.newKubernetesClient(k8sClusterConfig);
      Watcher podWatcher = watcherFactory.createPodWatcher(client, params);
      Watcher nodeWatcher = watcherFactory.createNodeWatcher(client, params);
      return NodePodWatcher.builder().nodeWatcher(nodeWatcher).podWatcher(podWatcher).build();
    });

    return watchId;
  }

  public void delete(String watchId) {
    // computeIfPresent form required for lookup & delete to be atomic.
    watchMap.computeIfPresent(watchId, (id, nodePodWatcher) -> {
      nodePodWatcher.nodeWatcher.onClose(null);
      nodePodWatcher.podWatcher.onClose(null);
      return null;
    });
  }
}
