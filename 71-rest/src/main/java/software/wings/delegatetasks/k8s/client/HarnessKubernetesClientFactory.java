package software.wings.delegatetasks.k8s.client;

import static java.util.Collections.emptyList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.Adapters;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.exception.InvalidRequestException;
import software.wings.beans.KubernetesConfig;
import software.wings.delegatetasks.k8s.exception.K8sClusterException;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.impl.KubernetesHelperService;

import java.net.MalformedURLException;
import java.net.URL;

@Singleton
public class HarnessKubernetesClientFactory implements KubernetesClientFactory {
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private KubernetesHelperService kubernetesHelperService;

  @Override
  public KubernetesClient newKubernetesClient(K8sClusterConfig k8sClusterConfig) {
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);
    kubernetesConfig.setMasterUrl(modifyMasterUrl(kubernetesConfig.getMasterUrl()));
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, emptyList());
  }

  @Override
  public <T extends Client> T newAdaptedClient(K8sClusterConfig k8sClusterConfig, Class<T> clazz) {
    final ExtensionAdapter<T> adapter = Adapters.get(clazz);
    final KubernetesClient client = newKubernetesClient(k8sClusterConfig);
    if (adapter != null && adapter.isAdaptable(client)) {
      return adapter.adapt(client);
    }
    throw new K8sClusterException("Cluster doesn't support metrics-server");
  }

  String modifyMasterUrl(String masterURL) {
    URL url;
    try {
      url = new URL(masterURL);
    } catch (MalformedURLException e) {
      throw new InvalidRequestException("URL is incorrect, the method will return the URL as is", e);
    }
    if (url.getPort() == -1) {
      if (masterURL.endsWith("/")) {
        masterURL = masterURL.substring(0, masterURL.length() - 1);
      }
      if (url.getProtocol().equals("https")) {
        return masterURL.concat(":443");
      } else {
        return masterURL.concat(":80");
      }
    }
    return masterURL;
  }
}
