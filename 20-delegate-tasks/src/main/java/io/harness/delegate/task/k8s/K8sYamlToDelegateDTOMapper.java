package io.harness.delegate.task.k8s;

import static io.harness.k8s.KubernetesHelperService.getKubernetesConfigFromDefaultKubeConfigFile;
import static io.harness.k8s.KubernetesHelperService.getKubernetesConfigFromServiceAccount;
import static io.harness.k8s.KubernetesHelperService.isRunningInCluster;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesConfig.KubernetesConfigBuilder;
import io.harness.k8s.model.OidcGrantType;

@Singleton
public class K8sYamlToDelegateDTOMapper {
  public KubernetesConfig createKubernetesConfigFromClusterConfig(KubernetesClusterConfigDTO clusterConfigDTO) {
    return createKubernetesConfigFromClusterConfig(clusterConfigDTO, null);
  }

  public KubernetesConfig createKubernetesConfigFromClusterConfig(
      KubernetesClusterConfigDTO clusterConfigDTO, String namespace) {
    String namespaceNotBlank = isNotBlank(namespace) ? namespace : "default";
    KubernetesCredentialType kubernetesCredentialType = clusterConfigDTO.getKubernetesCredentialType();

    switch (kubernetesCredentialType) {
      case INHERIT_FROM_DELEGATE:
        if (isRunningInCluster()) {
          return getKubernetesConfigFromServiceAccount(namespaceNotBlank);
        } else {
          return getKubernetesConfigFromDefaultKubeConfigFile(namespaceNotBlank);
        }

      case MANUAL_CREDENTIALS:
        return getKubernetesConfigFromManualCredentials(
            (KubernetesClusterDetailsDTO) (clusterConfigDTO.getConfig()), namespace);

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Kubernetes Credential type: [%s]", kubernetesCredentialType));
    }
  }

  private KubernetesConfig getKubernetesConfigFromManualCredentials(
      KubernetesClusterDetailsDTO clusterDetailsDTO, String namespace) {
    KubernetesConfigBuilder kubernetesConfigBuilder =
        KubernetesConfig.builder().masterUrl(clusterDetailsDTO.getMasterUrl()).namespace(namespace);

    // ToDo This does not handle the older KubernetesClusterConfigs which do not have authType set.
    KubernetesAuthDTO authDTO = clusterDetailsDTO.getAuth();
    switch (authDTO.getAuthType()) {
      case USER_PASSWORD:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.USER_PASSWORD);
        KubernetesUserNamePasswordDTO userNamePasswordDTO = (KubernetesUserNamePasswordDTO) authDTO.getCredentials();
        kubernetesConfigBuilder.username(userNamePasswordDTO.getUsername().toCharArray());
        kubernetesConfigBuilder.password(userNamePasswordDTO.getPasswordRef().getDecryptedValue());
        break;

      case CLIENT_KEY_CERT:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.CLIENT_KEY_CERT);
        KubernetesClientKeyCertDTO clientKeyCertDTO = (KubernetesClientKeyCertDTO) authDTO.getCredentials();
        kubernetesConfigBuilder.clientCert(clientKeyCertDTO.getClientCertRef().getDecryptedValue());
        kubernetesConfigBuilder.clientKey(clientKeyCertDTO.getClientKeyRef().getDecryptedValue());
        kubernetesConfigBuilder.clientKeyPassphrase(clientKeyCertDTO.getClientKeyPassphraseRef().getDecryptedValue());
        kubernetesConfigBuilder.clientKeyAlgo(clientKeyCertDTO.getClientKeyAlgo());
        kubernetesConfigBuilder.caCert(
            clientKeyCertDTO.getCaCertRef() != null ? clientKeyCertDTO.getCaCertRef().getDecryptedValue() : null);
        break;

      case SERVICE_ACCOUNT:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.SERVICE_ACCOUNT);
        KubernetesServiceAccountDTO serviceAccountDTO = (KubernetesServiceAccountDTO) authDTO.getCredentials();
        kubernetesConfigBuilder.serviceAccountToken(serviceAccountDTO.getServiceAccountTokenRef().getDecryptedValue());
        break;

      case OPEN_ID_CONNECT:
        kubernetesConfigBuilder.authType(KubernetesClusterAuthType.OIDC);
        KubernetesOpenIdConnectDTO openIdConnectDTO = (KubernetesOpenIdConnectDTO) authDTO.getCredentials();

        kubernetesConfigBuilder.oidcClientId(openIdConnectDTO.getOidcClientIdRef().getDecryptedValue());
        kubernetesConfigBuilder.oidcSecret(openIdConnectDTO.getOidcSecretRef() != null
                ? openIdConnectDTO.getOidcSecretRef().getDecryptedValue()
                : null);
        kubernetesConfigBuilder.oidcUsername(openIdConnectDTO.getOidcUsername());
        kubernetesConfigBuilder.oidcPassword(openIdConnectDTO.getOidcPasswordRef().getDecryptedValue());
        kubernetesConfigBuilder.oidcGrantType(OidcGrantType.password);
        kubernetesConfigBuilder.oidcIdentityProviderUrl(openIdConnectDTO.getOidcIssuerUrl());
        kubernetesConfigBuilder.oidcScopes(openIdConnectDTO.getOidcScopes());
        break;

      default:
        throw new UnsupportedOperationException(
            String.format("Unsupported Manual Credential type: [%s]", authDTO.getAuthType()));
    }
    return kubernetesConfigBuilder.build();
  }
}
