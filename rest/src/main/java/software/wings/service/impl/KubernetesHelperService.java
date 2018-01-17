package software.wings.service.impl;

import static org.apache.commons.lang.StringUtils.isNotBlank;

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
    String namespace = "default";
    ConfigBuilder configBuilder = new ConfigBuilder().withTrustCerts(true);
    if (isNotBlank(kubernetesConfig.getNamespace())) {
      namespace = kubernetesConfig.getNamespace().trim();
      configBuilder.withNamespace(namespace);
    }
    if (kubernetesConfig.getMasterUrl() != null) {
      configBuilder.withMasterUrl(kubernetesConfig.getMasterUrl().trim());
    }
    if (kubernetesConfig.getUsername() != null) {
      configBuilder.withUsername(kubernetesConfig.getUsername().trim());
    }
    if (kubernetesConfig.getPassword() != null) {
      configBuilder.withPassword(new String(kubernetesConfig.getPassword()).trim());
    }
    if (kubernetesConfig.getCaCert() != null) {
      configBuilder.withCaCertData(encode(kubernetesConfig.getCaCert()));
    }
    if (kubernetesConfig.getClientCert() != null) {
      configBuilder.withClientCertData(encode(kubernetesConfig.getClientCert()));
    }
    if (kubernetesConfig.getClientKey() != null) {
      configBuilder.withClientKeyData(encode(kubernetesConfig.getClientKey()));
    }
    if (kubernetesConfig.getClientKeyPassphrase() != null) {
      configBuilder.withClientKeyPassphrase(new String(kubernetesConfig.getClientKeyPassphrase()).trim());
    }
    if (kubernetesConfig.getClientKeyAlgo() != null) {
      configBuilder.withClientKeyAlgo(kubernetesConfig.getClientKeyAlgo().trim());
    }

    return new DefaultKubernetesClient(configBuilder.build()).inNamespace(namespace);
  }

  private String encode(char[] value) {
    return new String(value).trim();
    //    return new String(Base64.getEncoder().encode(new String(value).trim().getBytes()));
  }
}
