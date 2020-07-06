package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.OPEN_ID_CONNECT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.USER_PASSWORD;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.kubernetescluster.ClientKeyCertK8;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesAuth;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesCredential;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.entities.embedded.kubernetescluster.OpenIdConnectK8;
import io.harness.connector.entities.embedded.kubernetescluster.ServiceAccountK8;
import io.harness.connector.entities.embedded.kubernetescluster.UserNamePasswordK8;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.k8Connector.ClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.OpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.ServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.UserNamePasswordDTO;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnsupportedOperationException;

import java.util.Collections;

@Singleton
public class KubernetesDTOToEntity {
  public KubernetesClusterConfig toKubernetesClusterConfig(KubernetesClusterConfigDTO k8ClusterDTO) {
    KubernetesCredentialType credentialType = getKubernetesCredentialType(k8ClusterDTO);
    KubernetesCredential kubernetesCredential = getKubernetesCredential(k8ClusterDTO);
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().credentialType(credentialType).credential(kubernetesCredential).build();
    kubernetesClusterConfig.setCategories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER));
    kubernetesClusterConfig.setType(KUBERNETES_CLUSTER);
    return kubernetesClusterConfig;
  }

  private KubernetesCredentialType getKubernetesCredentialType(KubernetesClusterConfigDTO k8ClusterDTO) {
    return k8ClusterDTO.getKubernetesCredentialType();
  }

  private KubernetesCredential getKubernetesCredential(KubernetesClusterConfigDTO k8ClusterDTO) {
    KubernetesCredentialType k8CredentialType = k8ClusterDTO.getKubernetesCredentialType();
    if (k8CredentialType == INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO kubernetesDelegateDetails =
          castToKubernetesDelegateDetails(k8ClusterDTO.getConfig());
      return KubernetesDelegateDetails.builder().delegateName(kubernetesDelegateDetails.getDelegateName()).build();
    } else if (k8CredentialType == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO kubernetesClusterDetails = castToKubernetesClusterDetails(k8ClusterDTO.getConfig());
      return getKubernetesManualDetails(kubernetesClusterDetails);
    } else {
      throw new UnsupportedOperationException(
          String.format("The kubernetes credential type [%s] is invalid", k8CredentialType));
    }
  }

  private KubernetesDelegateDetailsDTO castToKubernetesDelegateDetails(KubernetesCredentialDTO k8ClusterDetails) {
    try {
      return (KubernetesDelegateDetailsDTO) k8ClusterDetails;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The kubernetes type and details doesn't match, expected [%s] type details", INHERIT_FROM_DELEGATE),
          ex);
    }
  }

  private KubernetesClusterDetailsDTO castToKubernetesClusterDetails(KubernetesCredentialDTO k8ClusterDetails) {
    try {
      return (KubernetesClusterDetailsDTO) k8ClusterDetails;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The kubernetes type and details doesn't match, expected [%s] type details", MANUAL_CREDENTIALS),
          ex);
    }
  }

  private KubernetesClusterDetails getKubernetesManualDetails(KubernetesClusterDetailsDTO kubernetesCredentialDetails) {
    KubernetesAuthType kubernetesAuthType = kubernetesCredentialDetails.getAuth().getAuthType();
    KubernetesAuth kubernetesCredential = getManualKubernetesCredentials(
        kubernetesCredentialDetails.getAuth().getCredentials(), kubernetesCredentialDetails.getAuth().getAuthType());
    return KubernetesClusterDetails.builder()
        .masterUrl(kubernetesCredentialDetails.getMasterUrl())
        .authType(kubernetesAuthType)
        .auth(kubernetesCredential)
        .build();
  }

  private KubernetesAuth getManualKubernetesCredentials(
      KubernetesAuthCredentialDTO kubernetesAuthCredentialDTO, KubernetesAuthType kubernetesAuthType) {
    switch (kubernetesAuthType) {
      case USER_PASSWORD:
        UserNamePasswordDTO userNamePasswordDTO = castToUserNamePasswordDTO(kubernetesAuthCredentialDTO);
        return toUserNamePasswordKubernetesCredential(userNamePasswordDTO);
      case CLIENT_KEY_CERT:
        ClientKeyCertDTO clientKeyCertDTO = castToClientKeyCertDTO(kubernetesAuthCredentialDTO);
        return toClientKeyCertKubernetesCredential(clientKeyCertDTO);
      case SERVICE_ACCOUNT:
        ServiceAccountDTO serviceAccountDTO = castToServiceAccountDTO(kubernetesAuthCredentialDTO);
        return toServiceAccountKubernetesCredential(serviceAccountDTO);
      case OPEN_ID_CONNECT:
        OpenIdConnectDTO openIdConnectDTO = castToOpenIdConnectDTO(kubernetesAuthCredentialDTO);
        return toOpenIdConnectKubernetesCredential(openIdConnectDTO);
      default:
        throw new UnsupportedOperationException(
            String.format("The manual credential type [%s] is invalid", kubernetesAuthType));
    }
  }

  private KubernetesAuth toUserNamePasswordKubernetesCredential(UserNamePasswordDTO userNamePasswordDTO) {
    return UserNamePasswordK8.builder()
        .userName(userNamePasswordDTO.getUsername())
        .password(userNamePasswordDTO.getPassword())
        .cacert(userNamePasswordDTO.getCacert())
        .build();
  }

  private KubernetesAuth toClientKeyCertKubernetesCredential(ClientKeyCertDTO clientKeyCertDTO) {
    return ClientKeyCertK8.builder()
        .clientKey(clientKeyCertDTO.getClientKey())
        .clientCert(clientKeyCertDTO.getClientCert())
        .clientKeyPassphrase(clientKeyCertDTO.getClientKeyPassphrase())
        .clientKeyAlgo(clientKeyCertDTO.getClientKeyAlgo())
        .build();
  }

  private KubernetesAuth toServiceAccountKubernetesCredential(ServiceAccountDTO serviceAccountDTO) {
    return ServiceAccountK8.builder().serviceAcccountToken(serviceAccountDTO.getServiceAccountToken()).build();
  }

  private KubernetesAuth toOpenIdConnectKubernetesCredential(OpenIdConnectDTO openIdConnectDTO) {
    return OpenIdConnectK8.builder()
        .oidcUsername(openIdConnectDTO.getOidcUsername())
        .oidcSecret(openIdConnectDTO.getOidcSecret())
        .oidcScopes(openIdConnectDTO.getOidcScopes())
        .oidcPassword(openIdConnectDTO.getOidcPassword())
        .oidcScopes(openIdConnectDTO.getOidcScopes())
        .oidcClientId(openIdConnectDTO.getOidcClientId())
        .oidcIssuerUrl(openIdConnectDTO.getOidcIssuerUrl())
        .build();
  }

  private UserNamePasswordDTO castToUserNamePasswordDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (UserNamePasswordDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private ClientKeyCertDTO castToClientKeyCertDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (ClientKeyCertDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", CLIENT_KEY_CERT),
          ex);
    }
  }

  private ServiceAccountDTO castToServiceAccountDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (ServiceAccountDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", SERVICE_ACCOUNT),
          ex);
    }
  }

  private OpenIdConnectDTO castToOpenIdConnectDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (OpenIdConnectDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", OPEN_ID_CONNECT),
          ex);
    }
  }
}
