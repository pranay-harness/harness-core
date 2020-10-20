package software.wings.delegatetasks.citasks.cik8handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.SecretDecryptionService;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.ci.pod.ConnectorDetails;

@Singleton
public class K8sConnectorHelper {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;

  public KubernetesClient createKubernetesClient(ConnectorDetails k8sConnectorDetails) {
    ConnectorDTO connectorDTO = k8sConnectorDetails.getConnectorDTO();
    KubernetesClusterConfigDTO clusterConfigDTO =
        (KubernetesClusterConfigDTO) connectorDTO.getConnectorInfo().getConnectorConfig();

    KubernetesCredentialSpecDTO credentialSpecDTO = clusterConfigDTO.getCredential().getConfig();
    KubernetesCredentialType kubernetesCredentialType = clusterConfigDTO.getCredential().getKubernetesCredentialType();
    if (kubernetesCredentialType == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesCredentialAuth =
          ((KubernetesClusterDetailsDTO) credentialSpecDTO).getAuth().getCredentials();
      secretDecryptionService.decrypt(kubernetesCredentialAuth, k8sConnectorDetails.getEncryptedDataDetails());
    }
    KubernetesConfig kubernetesConfig =
        k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(clusterConfigDTO);

    return kubernetesHelperService.getKubernetesClient(kubernetesConfig);
  }

  public DefaultKubernetesClient getDefaultKubernetesClient(ConnectorDetails k8sConnectorDetails) {
    ConnectorDTO connectorDTO = k8sConnectorDetails.getConnectorDTO();
    KubernetesClusterConfigDTO clusterConfigDTO =
        (KubernetesClusterConfigDTO) connectorDTO.getConnectorInfo().getConnectorConfig();
    KubernetesAuthCredentialDTO kubernetesCredentialAuth =
        ((KubernetesClusterDetailsDTO) clusterConfigDTO.getCredential().getConfig()).getAuth().getCredentials();
    secretDecryptionService.decrypt(kubernetesCredentialAuth, k8sConnectorDetails.getEncryptedDataDetails());
    KubernetesConfig kubernetesConfig =
        k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(clusterConfigDTO);

    Config config = kubernetesHelperService.getConfig(kubernetesConfig, StringUtils.EMPTY);
    return new DefaultKubernetesClient(kubernetesHelperService.createHttpClientWithProxySetting(config), config);
  }
}
