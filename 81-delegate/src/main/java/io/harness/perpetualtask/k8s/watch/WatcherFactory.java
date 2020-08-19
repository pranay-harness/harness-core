package io.harness.perpetualtask.k8s.watch;

import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;

public interface WatcherFactory {
  PodWatcher createPodWatcher(ApiClient apiClient, ClusterDetails params, K8sControllerFetcher controllerFetcher,
      SharedInformerFactory sharedInformerFactory, PVCFetcher pvcFetcher);
  NodeWatcher createNodeWatcher(
      ApiClient apiClient, ClusterDetails params, SharedInformerFactory sharedInformerFactory);
  PVCFetcher createPVCFetcher(ApiClient apiClient, SharedInformerFactory sharedInformerFactory);
}
