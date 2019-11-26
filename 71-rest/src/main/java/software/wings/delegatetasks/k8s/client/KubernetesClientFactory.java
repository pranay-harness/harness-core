package software.wings.delegatetasks.k8s.client;

import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.KubernetesClient;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

public interface KubernetesClientFactory {
  KubernetesClient newKubernetesClient(K8sClusterConfig k8sClusterConfig);

  <T extends Client> T newAdaptedClient(K8sClusterConfig k8sClusterConfig, Class<T> clazz);
}
