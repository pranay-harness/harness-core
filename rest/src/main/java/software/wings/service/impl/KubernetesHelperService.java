package software.wings.service.impl;

import static java.util.Collections.emptyList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import software.wings.beans.KubernetesConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

/**
 * Created by brett on 2/22/17
 */
@Singleton
public class KubernetesHelperService {
  @Inject private EncryptionService encryptionService;
  /**
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(kubernetesConfig, encryptedDataDetails);
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
    getKubernetesClient(kubernetesConfig, emptyList()).replicationControllers().list();
  }
}
