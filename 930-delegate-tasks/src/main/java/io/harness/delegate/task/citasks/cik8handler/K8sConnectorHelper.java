package io.harness.delegate.task.citasks.cik8handler;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@Singleton
public class K8sConnectorHelper {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;

  public KubernetesClient createKubernetesClient(ConnectorDetails k8sConnectorDetails) {
    KubernetesConfig kubernetesConfig = getKubernetesConfig(k8sConnectorDetails);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig);
  }

  public KubernetesConfig getKubernetesConfig(ConnectorDetails k8sConnectorDetails) {
    return getKubernetesConfig((KubernetesClusterConfigDTO) k8sConnectorDetails.getConnectorConfig(),
        k8sConnectorDetails.getEncryptedDataDetails());
  }

  public KubernetesConfig getKubernetesConfig(
      KubernetesClusterConfigDTO clusterConfigDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    KubernetesCredentialSpecDTO credentialSpecDTO = clusterConfigDTO.getCredential().getConfig();
    KubernetesCredentialType kubernetesCredentialType = clusterConfigDTO.getCredential().getKubernetesCredentialType();
    if (kubernetesCredentialType == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesCredentialAuth =
          ((KubernetesClusterDetailsDTO) credentialSpecDTO).getAuth().getCredentials();
      secretDecryptionService.decrypt(kubernetesCredentialAuth, encryptedDataDetails);
    }
    return k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(clusterConfigDTO);
  }

  public DefaultKubernetesClient getDefaultKubernetesClient(ConnectorDetails k8sConnectorDetails) {
    KubernetesClusterConfigDTO clusterConfigDTO = (KubernetesClusterConfigDTO) k8sConnectorDetails.getConnectorConfig();
    KubernetesAuthCredentialDTO kubernetesCredentialAuth =
        ((KubernetesClusterDetailsDTO) clusterConfigDTO.getCredential().getConfig()).getAuth().getCredentials();
    secretDecryptionService.decrypt(kubernetesCredentialAuth, k8sConnectorDetails.getEncryptedDataDetails());
    KubernetesConfig kubernetesConfig =
        k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(clusterConfigDTO);

    Config config = kubernetesHelperService.getConfig(kubernetesConfig, StringUtils.EMPTY);
    return new DefaultKubernetesClient(kubernetesHelperService.createHttpClientWithProxySetting(config), config);
  }
}
