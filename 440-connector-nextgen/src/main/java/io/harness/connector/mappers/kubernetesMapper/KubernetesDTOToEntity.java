/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Singleton;

@Singleton
public class KubernetesDTOToEntity
    implements ConnectorDTOToEntityMapper<KubernetesClusterConfigDTO, KubernetesClusterConfig> {
  @Override
  public KubernetesClusterConfig toConnectorEntity(KubernetesClusterConfigDTO k8ClusterDTO) {
    KubernetesCredentialType credentialType = getKubernetesCredentialType(k8ClusterDTO);
    KubernetesCredential kubernetesCredential = getKubernetesCredential(k8ClusterDTO);
    KubernetesClusterConfig kubernetesClusterConfig =
        KubernetesClusterConfig.builder().credentialType(credentialType).credential(kubernetesCredential).build();
    kubernetesClusterConfig.setType(KUBERNETES_CLUSTER);
    return kubernetesClusterConfig;
  }

  private KubernetesCredentialType getKubernetesCredentialType(KubernetesClusterConfigDTO k8ClusterDTO) {
    return k8ClusterDTO.getCredential().getKubernetesCredentialType();
  }

  private KubernetesCredential getKubernetesCredential(KubernetesClusterConfigDTO k8ClusterDTO) {
    KubernetesCredentialType k8CredentialType = getKubernetesCredentialType(k8ClusterDTO);
    if (k8CredentialType == INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO kubernetesDelegateDetails =
          castToKubernetesDelegateDetails(k8ClusterDTO.getCredential().getConfig());
      return null;
    } else if (k8CredentialType == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO kubernetesClusterDetails =
          castToKubernetesClusterDetails(k8ClusterDTO.getCredential().getConfig());
      return getKubernetesManualDetails(kubernetesClusterDetails);
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
        KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO =
            castToUserNamePasswordDTO(kubernetesAuthCredentialDTO);
        return toUserNamePasswordKubernetesCredential(kubernetesUserNamePasswordDTO);
      case CLIENT_KEY_CERT:
        KubernetesClientKeyCertDTO kubernetesClientKeyCertDTO = castToClientKeyCertDTO(kubernetesAuthCredentialDTO);
        return toClientKeyCertKubernetesCredential(kubernetesClientKeyCertDTO);
      case SERVICE_ACCOUNT:
        KubernetesServiceAccountDTO kubernetesServiceAccountDTO = castToServiceAccountDTO(kubernetesAuthCredentialDTO);
        return toServiceAccountKubernetesCredential(kubernetesServiceAccountDTO);
      case OPEN_ID_CONNECT:
        KubernetesOpenIdConnectDTO kubernetesOpenIdConnectDTO = castToOpenIdConnectDTO(kubernetesAuthCredentialDTO);
        return toOpenIdConnectKubernetesCredential(kubernetesOpenIdConnectDTO);
      default:
        throw new UnknownEnumTypeException("Kubernetes Manual Credential type",
            kubernetesAuthType == null ? null : kubernetesAuthType.getDisplayName());
    }
  }

  private KubernetesAuth toUserNamePasswordKubernetesCredential(
      KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO) {
    return K8sUserNamePassword.builder()
        .userName(kubernetesUserNamePasswordDTO.getUsername())
        .userNameRef(SecretRefHelper.getSecretConfigString(kubernetesUserNamePasswordDTO.getUsernameRef()))
        .passwordRef(SecretRefHelper.getSecretConfigString(kubernetesUserNamePasswordDTO.getPasswordRef()))
        .build();
  }

  private KubernetesAuth toClientKeyCertKubernetesCredential(KubernetesClientKeyCertDTO kubernetesClientKeyCertDTO) {
    return K8sClientKeyCert.builder()
        .clientKeyRef(SecretRefHelper.getSecretConfigString(kubernetesClientKeyCertDTO.getClientKeyRef()))
        .clientCertRef(SecretRefHelper.getSecretConfigString(kubernetesClientKeyCertDTO.getClientCertRef()))
        .clientKeyPassphraseRef(
            SecretRefHelper.getSecretConfigString(kubernetesClientKeyCertDTO.getClientKeyPassphraseRef()))
        .clientKeyAlgo(kubernetesClientKeyCertDTO.getClientKeyAlgo())
        .caCertRef(SecretRefHelper.getSecretConfigString(kubernetesClientKeyCertDTO.getCaCertRef()))
        .build();
  }

  private KubernetesAuth toServiceAccountKubernetesCredential(KubernetesServiceAccountDTO kubernetesServiceAccountDTO) {
    return K8sServiceAccount.builder()
        .serviceAcccountTokenRef(
            SecretRefHelper.getSecretConfigString(kubernetesServiceAccountDTO.getServiceAccountTokenRef()))
        .build();
  }

  private KubernetesAuth toOpenIdConnectKubernetesCredential(KubernetesOpenIdConnectDTO kubernetesOpenIdConnectDTO) {
    return K8sOpenIdConnect.builder()
        .oidcUsername(kubernetesOpenIdConnectDTO.getOidcUsername())
        .oidcUsernameRef(SecretRefHelper.getSecretConfigString(kubernetesOpenIdConnectDTO.getOidcUsernameRef()))
        .oidcSecretRef(SecretRefHelper.getSecretConfigString(kubernetesOpenIdConnectDTO.getOidcSecretRef()))
        .oidcScopes(kubernetesOpenIdConnectDTO.getOidcScopes())
        .oidcPasswordRef(SecretRefHelper.getSecretConfigString(kubernetesOpenIdConnectDTO.getOidcPasswordRef()))
        .oidcScopes(kubernetesOpenIdConnectDTO.getOidcScopes())
        .oidcClientIdRef(SecretRefHelper.getSecretConfigString(kubernetesOpenIdConnectDTO.getOidcClientIdRef()))
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
