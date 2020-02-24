package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.informer.SharedInformerFactoryFactory;
import io.harness.serializer.KryoUtils;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.k8s.apiclient.ApiClientFactory;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class K8sWatchServiceDelegate {
  private final WatcherFactory watcherFactory;
  private final KubernetesClientFactory kubernetesClientFactory;
  private final SharedInformerFactoryFactory sharedInformerFactoryFactory;
  private final ApiClientFactory apiClientFactory;

  private final Map<String, WatcherGroup> watchMap; // <id, Watch>

  @Inject
  public K8sWatchServiceDelegate(WatcherFactory watcherFactory, KubernetesClientFactory kubernetesClientFactory,
      SharedInformerFactoryFactory sharedInformerFactoryFactory, ApiClientFactory apiClientFactory) {
    this.watcherFactory = watcherFactory;
    this.kubernetesClientFactory = kubernetesClientFactory;
    this.sharedInformerFactoryFactory = sharedInformerFactoryFactory;
    this.apiClientFactory = apiClientFactory;
    this.watchMap = new ConcurrentHashMap<>();
  }

  @Builder
  static class WatcherGroup implements Closeable {
    private Watcher<Node> nodeWatcher;
    private Watcher<Pod> podWatcher;
    private Watcher<Event> clusterEventWatcher;
    private SharedInformerFactory sharedInformerFactory;

    @Override
    public void close() {
      nodeWatcher.onClose(null);
      podWatcher.onClose(null);
      clusterEventWatcher.onClose(null);
      sharedInformerFactory.stopAllRegisteredInformers();
    }
  }

  Iterable<String> watchIds() {
    return watchMap.keySet();
  }

  public String create(K8sWatchTaskParams params) {
    String watchId = params.getClusterId();
    watchMap.computeIfAbsent(watchId, id -> {
      logger.info("Creating watch with id: {}", watchId);
      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) KryoUtils.asObject(params.getK8SClusterConfig().toByteArray());
      KubernetesClient client = kubernetesClientFactory.newKubernetesClient(k8sClusterConfig);
      ApiClient apiClient = apiClientFactory.getClient(k8sClusterConfig);
      Watcher<Pod> podWatcher = watcherFactory.createPodWatcher(client, params);
      Watcher<Node> nodeWatcher = watcherFactory.createNodeWatcher(client, params);
      Watcher<Event> eventWatcher = watcherFactory.createClusterEventWatcher(client, params);
      SharedInformerFactory sharedInformerFactory = sharedInformerFactoryFactory.createSharedInformerFactory(apiClient,
          ClusterDetails.builder()
              .cloudProviderId(params.getCloudProviderId())
              .clusterId(params.getClusterId())
              .clusterName(params.getClusterName())
              .build());
      sharedInformerFactory.startAllRegisteredInformers();
      return WatcherGroup.builder()
          .nodeWatcher(nodeWatcher)
          .podWatcher(podWatcher)
          .clusterEventWatcher(eventWatcher)
          .sharedInformerFactory(sharedInformerFactory)
          .build();
    });

    return watchId;
  }

  public void delete(String watchId) {
    watchMap.computeIfPresent(watchId, (id, watcherGroup) -> {
      logger.info("Deleting watch with id: {}", watchId);
      watcherGroup.close();
      return null;
    });
  }
}
