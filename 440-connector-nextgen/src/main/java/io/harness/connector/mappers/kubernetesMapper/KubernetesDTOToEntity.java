package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.CLIENT_KEY_CERT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.OPEN_ID_CONNECT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.USER_PASSWORD;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;

import io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert;
import io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect;
import io.harness.connector.entities.embedded.kubernetescluster.K8sServiceAccount;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesAuth;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesCredential;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class KubernetesDTOToEntity
    implements ConnectorDTOToEntityMapper<KubernetesClusterConfigDTO, KubernetesClusterConfig> {
  private SecretRefService secretRefService;

  @Override
  public KubernetesClusterConfig toConnectorEntity(KubernetesClusterConfigDTO k8ClusterDTO, NGAccess ngAccess) {
    KubernetesCredentialType credentialType = getKubernetesCredentialType(k8ClusterDTO);
    KubernetesCredential kubernetesCredential = getKubernetesCredential(k8ClusterDTO, ngAccess);
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().credentialType(credentialType).credential(kubernetesCredential).build();
    kubernetesClusterConfig.setType(KUBERNETES_CLUSTER);
    return kubernetesClusterConfig;
  }

  private KubernetesCredentialType getKubernetesCredentialType(KubernetesClusterConfigDTO k8ClusterDTO) {
    return k8ClusterDTO.getCredential().getKubernetesCredentialType();
  }

  private KubernetesCredential getKubernetesCredential(KubernetesClusterConfigDTO k8ClusterDTO, NGAccess ngAccess) {
    KubernetesCredentialType k8CredentialType = getKubernetesCredentialType(k8ClusterDTO);
    if (k8CredentialType == INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO kubernetesDelegateDetails =
          castToKubernetesDelegateDetails(k8ClusterDTO.getCredential().getConfig());
      return KubernetesDelegateDetails.builder()
          .delegateSelectors(kubernetesDelegateDetails.getDelegateSelectors())
          .build();
    } else if (k8CredentialType == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO kubernetesClusterDetails =
          castToKubernetesClusterDetails(k8ClusterDTO.getCredential().getConfig());
      return getKubernetesManualDetails(kubernetesClusterDetails, ngAccess);
    } else {
      throw new UnknownEnumTypeException(
          "Kubernetes credential type", k8CredentialType == null ? null : k8CredentialType.getDisplayName());
    }
  }

  private KubernetesDelegateDetailsDTO castToKubernetesDelegateDetails(KubernetesCredentialSpecDTO k8ClusterDetails) {
    try {
      return (KubernetesDelegateDetailsDTO) k8ClusterDetails;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The kubernetes type and details doesn't match, expected [%s] type details", INHERIT_FROM_DELEGATE),
          ex);
    }
  }

  private KubernetesClusterDetailsDTO castToKubernetesClusterDetails(KubernetesCredentialSpecDTO k8ClusterDetails) {
    try {
      return (KubernetesClusterDetailsDTO) k8ClusterDetails;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The kubernetes type and details doesn't match, expected [%s] type details", MANUAL_CREDENTIALS),
          ex);
    }
  }

  private KubernetesClusterDetails getKubernetesManualDetails(
      KubernetesClusterDetailsDTO kubernetesCredentialDetails, NGAccess ngAccess) {
    KubernetesAuthType kubernetesAuthType = kubernetesCredentialDetails.getAuth().getAuthType();
    KubernetesAuth kubernetesCredential =
        getManualKubernetesCredentials(kubernetesCredentialDetails.getAuth().getCredentials(),
            kubernetesCredentialDetails.getAuth().getAuthType(), ngAccess);
    return KubernetesClusterDetails.builder()
        .masterUrl(kubernetesCredentialDetails.getMasterUrl())
        .authType(kubernetesAuthType)
        .auth(kubernetesCredential)
        .build();
  }

  private KubernetesAuth getManualKubernetesCredentials(KubernetesAuthCredentialDTO kubernetesAuthCredentialDTO,
      KubernetesAuthType kubernetesAuthType, NGAccess ngAccess) {
    switch (kubernetesAuthType) {
      case USER_PASSWORD:
        KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO =
            castToUserNamePasswordDTO(kubernetesAuthCredentialDTO);
        return toUserNamePasswordKubernetesCredential(kubernetesUserNamePasswordDTO, ngAccess);
      case CLIENT_KEY_CERT:
        KubernetesClientKeyCertDTO kubernetesClientKeyCertDTO = castToClientKeyCertDTO(kubernetesAuthCredentialDTO);
        return toClientKeyCertKubernetesCredential(kubernetesClientKeyCertDTO, ngAccess);
      case SERVICE_ACCOUNT:
        KubernetesServiceAccountDTO kubernetesServiceAccountDTO = castToServiceAccountDTO(kubernetesAuthCredentialDTO);
        return toServiceAccountKubernetesCredential(kubernetesServiceAccountDTO, ngAccess);
      case OPEN_ID_CONNECT:
        KubernetesOpenIdConnectDTO kubernetesOpenIdConnectDTO = castToOpenIdConnectDTO(kubernetesAuthCredentialDTO);
        return toOpenIdConnectKubernetesCredential(kubernetesOpenIdConnectDTO, ngAccess);
      default:
        throw new UnknownEnumTypeException("Kubernetes Manual Credential type",
            kubernetesAuthType == null ? null : kubernetesAuthType.getDisplayName());
    }
  }

  private KubernetesAuth toUserNamePasswordKubernetesCredential(
      KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO, NGAccess ngAccess) {
    return K8sUserNamePassword.builder()
        .userName(kubernetesUserNamePasswordDTO.getUsername())
        .userNameRef(
            secretRefService.validateAndGetSecretConfigString(kubernetesUserNamePasswordDTO.getUsernameRef(), ngAccess))
        .passwordRef(
            secretRefService.validateAndGetSecretConfigString(kubernetesUserNamePasswordDTO.getPasswordRef(), ngAccess))
        .build();
  }

  private KubernetesAuth toClientKeyCertKubernetesCredential(
      KubernetesClientKeyCertDTO kubernetesClientKeyCertDTO, NGAccess ngAccess) {
    return K8sClientKeyCert.builder()
        .clientKeyRef(
            secretRefService.validateAndGetSecretConfigString(kubernetesClientKeyCertDTO.getClientKeyRef(), ngAccess))
        .clientCertRef(
            secretRefService.validateAndGetSecretConfigString(kubernetesClientKeyCertDTO.getClientCertRef(), ngAccess))
        .clientKeyPassphraseRef(secretRefService.validateAndGetSecretConfigString(
            kubernetesClientKeyCertDTO.getClientKeyPassphraseRef(), ngAccess))
        .clientKeyAlgo(kubernetesClientKeyCertDTO.getClientKeyAlgo())
        .caCertRef(
            secretRefService.validateAndGetSecretConfigString(kubernetesClientKeyCertDTO.getCaCertRef(), ngAccess))
        .build();
  }

  private KubernetesAuth toServiceAccountKubernetesCredential(
      KubernetesServiceAccountDTO kubernetesServiceAccountDTO, NGAccess ngAccess) {
    return K8sServiceAccount.builder()
        .serviceAcccountTokenRef(secretRefService.validateAndGetSecretConfigString(
            kubernetesServiceAccountDTO.getServiceAccountTokenRef(), ngAccess))
        .build();
  }

  private KubernetesAuth toOpenIdConnectKubernetesCredential(
      KubernetesOpenIdConnectDTO kubernetesOpenIdConnectDTO, NGAccess ngAccess) {
    return K8sOpenIdConnect.builder()
        .oidcUsername(kubernetesOpenIdConnectDTO.getOidcUsername())
        .oidcUsernameRef(secretRefService.validateAndGetSecretConfigString(
            kubernetesOpenIdConnectDTO.getOidcUsernameRef(), ngAccess))
        .oidcSecretRef(
            secretRefService.validateAndGetSecretConfigString(kubernetesOpenIdConnectDTO.getOidcSecretRef(), ngAccess))
        .oidcScopes(kubernetesOpenIdConnectDTO.getOidcScopes())
        .oidcPasswordRef(secretRefService.validateAndGetSecretConfigString(
            kubernetesOpenIdConnectDTO.getOidcPasswordRef(), ngAccess))
        .oidcScopes(kubernetesOpenIdConnectDTO.getOidcScopes())
        .oidcClientIdRef(secretRefService.validateAndGetSecretConfigString(
            kubernetesOpenIdConnectDTO.getOidcClientIdRef(), ngAccess))
        .oidcIssuerUrl(kubernetesOpenIdConnectDTO.getOidcIssuerUrl())
        .build();
  }

  private KubernetesUserNamePasswordDTO castToUserNamePasswordDTO(
      KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (KubernetesUserNamePasswordDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private KubernetesClientKeyCertDTO castToClientKeyCertDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (KubernetesClientKeyCertDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", CLIENT_KEY_CERT),
          ex);
    }
  }

  private KubernetesServiceAccountDTO castToServiceAccountDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (KubernetesServiceAccountDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", SERVICE_ACCOUNT),
          ex);
    }
  }

  private KubernetesOpenIdConnectDTO castToOpenIdConnectDTO(KubernetesAuthCredentialDTO kubernetesAuthCredentials) {
    try {
      return (KubernetesOpenIdConnectDTO) kubernetesAuthCredentials;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format(
              "The credential type and credentials doesn't match, expected [%s] credentials", OPEN_ID_CONNECT),
          ex);
    }
  }
}
