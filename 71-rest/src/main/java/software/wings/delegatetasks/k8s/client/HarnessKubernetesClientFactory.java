package software.wings.delegatetasks.k8s.client;

import static java.util.Collections.emptyList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import software.wings.beans.KubernetesConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.impl.KubernetesHelperService;

@Singleton
public class HarnessKubernetesClientFactory implements KubernetesClientFactory {
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private KubernetesHelperService kubernetesHelperService;

  @Override
  public KubernetesClient newKubernetesClient(K8sClusterConfig k8sClusterConfig) {
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, emptyList());
  }
}
