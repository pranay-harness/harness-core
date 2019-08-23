package software.wings.delegatetasks.k8s.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

public class Fabric8KubernetesClientFactory implements KubernetesClientFactory {
  @Override
  public KubernetesClient newKubernetesClient(K8sClusterConfig k8sClusterConfig) {
    Config config = new ConfigBuilder().build();
    return new DefaultKubernetesClient(config);
  }
}
