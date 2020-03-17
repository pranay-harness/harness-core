package io.harness.perpetualtask.k8s.watch;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;

public interface WatcherFactory {
  PodWatcher createPodWatcher(KubernetesClient client, ClusterDetails params);
  NodeWatcher createNodeWatcher(KubernetesClient client, ClusterDetails params);
  ClusterEventWatcher createClusterEventWatcher(KubernetesClient client, ClusterDetails params);
}
