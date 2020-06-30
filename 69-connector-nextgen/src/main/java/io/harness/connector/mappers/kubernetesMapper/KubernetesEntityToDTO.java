package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.connector.common.kubernetes.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.connector.common.kubernetes.KubernetesAuthType.OPEN_ID_CONNECT;
import static io.harness.connector.common.kubernetes.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.connector.common.kubernetes.KubernetesAuthType.USER_PASSWORD;
import static io.harness.connector.common.kubernetes.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.connector.common.kubernetes.KubernetesCredentialType.MANUAL_CREDENTIALS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dtos.K8Connector.ClientKeyCertDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesAuthDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterConfigDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesClusterDetailsDTO;
import io.harness.connector.apis.dtos.K8Connector.KubernetesDelegateDetailsDTO;
import io.harness.connector.apis.dtos.K8Connector.OpenIdConnectDTO;
import io.harness.connector.apis.dtos.K8Connector.ServiceAccountDTO;
import io.harness.connector.apis.dtos.K8Connector.UserNamePasswordDTO;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.ClientKeyCertK8;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesAuth;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterConfig;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesClusterDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.KubernetesDelegateDetails;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.OpenIdConnectK8;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.ServiceAccountK8;
import io.harness.connector.entities.connectorTypes.kubernetesCluster.UserNamePasswordK8;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnsupportedOperationException;

@Singleton
public class KubernetesEntityToDTO {
  @Inject private KubernetesConfigCastHelper kubernetesConfigCastHelper;
  public KubernetesClusterConfigDTO createK8ClusterConfigDTO(Connector connector) {
    KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) connector;
    String inheritConfigFromDelegate = null;
    if (kubernetesClusterConfig.getCredentialType() == INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetails kubernetesDelegateDetails =
          kubernetesConfigCastHelper.castToKubernetesDelegateCredential(kubernetesClusterConfig.getCredential());
      return createInheritFromDelegateCredentialsDTO(kubernetesDelegateDetails);
    } else if (kubernetesClusterConfig.getCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetails kubernetesClusterDetails =
          kubernetesConfigCastHelper.castToManualKubernetesCredentials(kubernetesClusterConfig.getCredential());
      return createManualKubernetessCredentialsDTO(kubernetesClusterDetails);
    } else {
      throw new UnsupportedOperationException(
          String.format("The kubernetes credential type [%s] is invalid", kubernetesClusterConfig.getCredentialType()));
    }
  }

  private KubernetesClusterConfigDTO createInheritFromDelegateCredentialsDTO(
      KubernetesDelegateDetails delegateCredential) {
    return KubernetesClusterConfigDTO.builder()
        .config(KubernetesDelegateDetailsDTO.builder().delegateName(delegateCredential.getDelegateName()).build())
        .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
        .build();
  }

  private KubernetesClusterConfigDTO createManualKubernetessCredentialsDTO(
      KubernetesClusterDetails kubernetesClusterDetails) {
    KubernetesAuthDTO manualCredentials = null;
    switch (kubernetesClusterDetails.getAuthType()) {
      case USER_PASSWORD:
        UserNamePasswordK8 userNamePasswordK8 = castToUserNamePassowordDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createUserPasswordDTO(userNamePasswordK8);
        break;
      case CLIENT_KEY_CERT:
        ClientKeyCertK8 clientKeyCertK8 = castToClientKeyCertDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createClientKeyCertDTO(clientKeyCertK8);
        break;
      case SERVICE_ACCOUNT:
        ServiceAccountK8 serviceAccountK8 = castToServiceAccountDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createServiceAccountDTO(serviceAccountK8);
        break;
      case OPEN_ID_CONNECT:
        OpenIdConnectK8 openIdConnectK8 = castToOpenIdConnectDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createOpenIdConnectDTO(openIdConnectK8);
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("The manual credential type [%s] is invalid", kubernetesClusterDetails.getAuthType()));
    }
    return KubernetesClusterConfigDTO.builder()
        .kubernetesCredentialType(MANUAL_CREDENTIALS)
        .config(KubernetesClusterDetailsDTO.builder()
                    .masterUrl(kubernetesClusterDetails.getMasterUrl())
                    .auth(manualCredentials)
                    .build())
        .build();
  }

  private KubernetesAuthDTO createUserPasswordDTO(UserNamePasswordK8 userNamePasswordCredential) {
    UserNamePasswordDTO userNamePasswordDTO = UserNamePasswordDTO.builder()
                                                  .username(userNamePasswordCredential.getUserName())
                                                  .password(userNamePasswordCredential.getPassword())
                                                  .cacert(userNamePasswordCredential.getCacert())
                                                  .build();
    return KubernetesAuthDTO.builder().authType(USER_PASSWORD).credentials(userNamePasswordDTO).build();
  }

  private KubernetesAuthDTO createClientKeyCertDTO(ClientKeyCertK8 clientKeyCertK8) {
    ClientKeyCertDTO clientKeyCertDTO = ClientKeyCertDTO.builder()
                                            .clientKey(clientKeyCertK8.getClientKey())
                                            .clientCert(clientKeyCertK8.getClientCert())
                                            .clientKeyPassphrase(clientKeyCertK8.getClientKeyPassphrase())
                                            .clientKeyAlgo(clientKeyCertK8.getClientKeyAlgo())
                                            .build();
    return KubernetesAuthDTO.builder().authType(CLIENT_KEY_CERT).credentials(clientKeyCertDTO).build();
  }

  private KubernetesAuthDTO createServiceAccountDTO(ServiceAccountK8 serviceAccountK8) {
    ServiceAccountDTO serviceAccountDTO =
        ServiceAccountDTO.builder().serviceAccountToken(serviceAccountK8.getServiceAcccountToken()).build();
    return KubernetesAuthDTO.builder().authType(SERVICE_ACCOUNT).credentials(serviceAccountDTO).build();
  }

  private KubernetesAuthDTO createOpenIdConnectDTO(OpenIdConnectK8 openIdConnectK8) {
    OpenIdConnectDTO openIdConnectDTO = OpenIdConnectDTO.builder()
                                            .oidcClientId(openIdConnectK8.getOidcClientId())
                                            .oidcIssuerUrl(openIdConnectK8.getOidcIssuerUrl())
                                            .oidcPassword(openIdConnectK8.getOidcPassword())
                                            .oidcScopes(openIdConnectK8.getOidcScopes())
                                            .oidcSecret(openIdConnectK8.getOidcSecret())
                                            .oidcUsername(openIdConnectK8.getOidcUsername())
                                            .build();
    return KubernetesAuthDTO.builder().authType(OPEN_ID_CONNECT).credentials(openIdConnectDTO).build();
  }

  private UserNamePasswordK8 castToUserNamePassowordDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (UserNamePasswordK8) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private ClientKeyCertK8 castToClientKeyCertDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (ClientKeyCertK8) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private ServiceAccountK8 castToServiceAccountDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (ServiceAccountK8) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private OpenIdConnectK8 castToOpenIdConnectDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (OpenIdConnectK8) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }
}
