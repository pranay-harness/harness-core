package io.harness.perpetualtask.k8s.watch;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface WatcherFactory {
  PodWatcher createPodWatcher(KubernetesClient client, K8sWatchTaskParams params);
  NodeWatcher createNodeWatcher(KubernetesClient client, K8sWatchTaskParams params);
  ClusterEventWatcher createClusterEventWatcher(KubernetesClient client, K8sWatchTaskParams params);
}
