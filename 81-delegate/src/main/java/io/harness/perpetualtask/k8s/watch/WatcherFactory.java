package io.harness.perpetualtask.k8s.watch;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface WatcherFactory {
  PodWatcher createPodWatcher(KubernetesClient client);
  NodeWatcher createNodeWatcher(KubernetesClient client);
}
