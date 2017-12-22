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
    if (!kubernetesConfig.isDecrypted()) {
      encryptionService.decrypt(kubernetesConfig, encryptedDataDetails);
    }
    ConfigBuilder configBuilder = new ConfigBuilder()
                                      .withMasterUrl(kubernetesConfig.getMasterUrl())
                                      .withTrustCerts(true)
                                      .withUsername(kubernetesConfig.getUsername())
                                      .withNamespace(kubernetesConfig.getNamespace());

    if (kubernetesConfig.getPassword() != null) {
      configBuilder.withPassword(new String(kubernetesConfig.getPassword()));
    }
    if (kubernetesConfig.getCaCert() != null) {
      configBuilder.withCaCertData(new String(kubernetesConfig.getCaCert()));
    }
    if (kubernetesConfig.getClientCert() != null) {
      configBuilder.withClientCertData(new String(kubernetesConfig.getClientCert()));
    }
    if (kubernetesConfig.getClientKey() != null) {
      configBuilder.withClientKeyData(new String(kubernetesConfig.getClientKey()));
    }

    return new DefaultKubernetesClient(configBuilder.build()).inNamespace(kubernetesConfig.getNamespace());
  }

  public void validateCredential(KubernetesConfig kubernetesConfig) {
    getKubernetesClient(kubernetesConfig, emptyList()).replicationControllers().list();
  }
}
