package io.harness.perpetualtask.k8s.watch;

import com.google.common.collect.ImmutableMap;
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
import io.kubernetes.client.informer.cache.Store;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1beta1CronJob;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.k8s.apiclient.ApiClientFactory;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactory;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class K8sWatchServiceDelegate {
  private static final Map<String, Class<?>> KNOWN_WORKLOAD_TYPES = ImmutableMap.<String, Class<?>>builder()
                                                                        .put("Deployment", V1Deployment.class)
                                                                        .put("ReplicaSet", V1ReplicaSet.class)
                                                                        .put("DaemonSet", V1DaemonSet.class)
                                                                        .put("StatefulSet", V1StatefulSet.class)
                                                                        .put("Job", V1Job.class)
                                                                        .put("CronJob", V1beta1CronJob.class)
                                                                        .build();

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

  @Value
  @Builder
  static class WatcherGroup implements Closeable {
    String watchId;
    Watcher<Node> nodeWatcher;
    Watcher<Pod> podWatcher;
    Watcher<Event> clusterEventWatcher;
    SharedInformerFactory sharedInformerFactory;

    @Override
    public void close() {
      logger.info("Closing watcher group for watch {}", watchId);
      sharedInformerFactory.stopAllRegisteredInformers();
      nodeWatcher.onClose(null);
      podWatcher.onClose(null);
      clusterEventWatcher.onClose(null);
    }
  }

  Iterable<String> watchIds() {
    return watchMap.keySet();
  }

  public String create(K8sWatchTaskParams params) {
    String watchId = params.getClusterId();
    watchMap.computeIfAbsent(watchId, id -> {
      logger.info("Creating watch with id: {}", id);
      K8sClusterConfig k8sClusterConfig =
          (K8sClusterConfig) KryoUtils.asObject(params.getK8SClusterConfig().toByteArray());
      KubernetesClient client = kubernetesClientFactory.newKubernetesClient(k8sClusterConfig);
      String kubeSystemUid = getKubeSystemUid(client);
      ClusterDetails clusterDetails = ClusterDetails.builder()
                                          .cloudProviderId(params.getCloudProviderId())
                                          .clusterId(params.getClusterId())
                                          .clusterName(params.getClusterName())
                                          .kubeSystemUid(kubeSystemUid)
                                          .build();
      ApiClient apiClient = apiClientFactory.getClient(k8sClusterConfig);
      SharedInformerFactory sharedInformerFactory =
          sharedInformerFactoryFactory.createSharedInformerFactory(apiClient, clusterDetails);
      sharedInformerFactory.startAllRegisteredInformers();
      Map<String, Store<?>> stores = KNOWN_WORKLOAD_TYPES.entrySet().stream().collect(Collectors.toMap(
          Map.Entry::getKey, e -> sharedInformerFactory.getExistingSharedIndexInformer(e.getValue()).getIndexer()));
      Watcher<Node> nodeWatcher = watcherFactory.createNodeWatcher(client, clusterDetails);
      Watcher<Event> eventWatcher = watcherFactory.createClusterEventWatcher(client, clusterDetails);
      K8sControllerFetcher controllerFetcher = new K8sControllerFetcher(stores);
      Watcher<Pod> podWatcher = watcherFactory.createPodWatcher(client, clusterDetails, controllerFetcher);
      return WatcherGroup.builder()
          .watchId(id)
          .nodeWatcher(nodeWatcher)
          .podWatcher(podWatcher)
          .clusterEventWatcher(eventWatcher)
          .sharedInformerFactory(sharedInformerFactory)
          .build();
    });

    return watchId;
  }

  public static String getKubeSystemUid(KubernetesClient client) {
    try {
      return client.namespaces().withName("kube-system").get().getMetadata().getUid();
    } catch (Exception e) {
      logger.warn("Error getting kube-system namespace uid", e);
      return "kube-system";
    }
  }

  public void delete(String watchId) {
    watchMap.computeIfPresent(watchId, (id, watcherGroup) -> {
      logger.info("Deleting watch with id: {}", watchId);
      watcherGroup.close();
      return null;
    });
  }
}
