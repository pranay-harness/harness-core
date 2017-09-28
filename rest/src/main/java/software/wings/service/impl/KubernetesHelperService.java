package software.wings.service.impl;

import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import software.wings.beans.KubernetesConfig;

/**
 * Created by brett on 2/22/17
 */
@Singleton
public class KubernetesHelperService {
  /**
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(KubernetesConfig kubernetesConfig) {
    return new DefaultKubernetesClient(
        new ConfigBuilder()
            .withMasterUrl(kubernetesConfig.getMasterUrl())
            .withTrustCerts(true)
            .withUsername(kubernetesConfig.getUsername())
            .withPassword(kubernetesConfig.getPassword() != null ? new String(kubernetesConfig.getPassword()) : "")
            .withCaCertData(kubernetesConfig.getCaCert())
            .withClientCertData(kubernetesConfig.getClientCert())
            .withClientKeyData(kubernetesConfig.getClientKey())
            .withNamespace(kubernetesConfig.getNamespace())
            .build())
        .inNamespace(kubernetesConfig.getNamespace());
  }

  public void validateCredential(KubernetesConfig kubernetesConfig) {
    getKubernetesClient(kubernetesConfig).replicationControllers().list();
  }
}
