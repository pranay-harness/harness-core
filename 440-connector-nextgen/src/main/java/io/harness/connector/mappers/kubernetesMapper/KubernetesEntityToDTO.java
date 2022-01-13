/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.kubernetesMapper;

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
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnknownEnumTypeException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class KubernetesEntityToDTO
    implements ConnectorEntityToDTOMapper<KubernetesClusterConfigDTO, KubernetesClusterConfig> {
  @Inject private KubernetesConfigCastHelper kubernetesConfigCastHelper;

  @Override
  public KubernetesClusterConfigDTO createConnectorDTO(KubernetesClusterConfig connector) {
    KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) connector;
    if (kubernetesClusterConfig.getCredentialType() == INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetails kubernetesDelegateDetails =
          kubernetesConfigCastHelper.castToKubernetesDelegateCredential(kubernetesClusterConfig.getCredential());
      return createInheritFromDelegateCredentialsDTO(kubernetesDelegateDetails);
    } else if (kubernetesClusterConfig.getCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetails kubernetesClusterDetails =
          kubernetesConfigCastHelper.castToManualKubernetesCredentials(kubernetesClusterConfig.getCredential());
      return createManualKubernetessCredentialsDTO(kubernetesClusterDetails);
    } else {
      throw new UnknownEnumTypeException("Kubernetes credential type",
          kubernetesClusterConfig.getCredentialType() == null
              ? null
              : kubernetesClusterConfig.getCredentialType().getDisplayName());
    }
  }

  private KubernetesClusterConfigDTO createInheritFromDelegateCredentialsDTO(
      KubernetesDelegateDetails delegateCredential) {
    KubernetesCredentialDTO k8sCredentials =
        KubernetesCredentialDTO.builder().config(null).kubernetesCredentialType(INHERIT_FROM_DELEGATE).build();
    return KubernetesClusterConfigDTO.builder().credential(k8sCredentials).build();
  }

  private KubernetesClusterConfigDTO createManualKubernetessCredentialsDTO(
      KubernetesClusterDetails kubernetesClusterDetails) {
    KubernetesAuthDTO manualCredentials = null;
    switch (kubernetesClusterDetails.getAuthType()) {
      case USER_PASSWORD:
        K8sUserNamePassword k8sUserNamePassword = castToUserNamePassowordDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createUserPasswordDTO(k8sUserNamePassword);
        break;
      case CLIENT_KEY_CERT:
        K8sClientKeyCert k8sClientKeyCert = castToClientKeyCertDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createClientKeyCertDTO(k8sClientKeyCert);
        break;
      case SERVICE_ACCOUNT:
        K8sServiceAccount k8sServiceAccount = castToServiceAccountDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createServiceAccountDTO(k8sServiceAccount);
        break;
      case OPEN_ID_CONNECT:
        K8sOpenIdConnect k8sOpenIdConnect = castToOpenIdConnectDTO(kubernetesClusterDetails.getAuth());
        manualCredentials = createOpenIdConnectDTO(k8sOpenIdConnect);
        break;
      default:
        throw new UnknownEnumTypeException("Kubernetes Manual Credential type",
            kubernetesClusterDetails.getAuthType() == null ? null
                                                           : kubernetesClusterDetails.getAuthType().getDisplayName());
    }
    KubernetesCredentialDTO k8sCredentials = KubernetesCredentialDTO.builder()
                                                 .kubernetesCredentialType(MANUAL_CREDENTIALS)
                                                 .config(KubernetesClusterDetailsDTO.builder()
                                                             .masterUrl(kubernetesClusterDetails.getMasterUrl())
                                                             .auth(manualCredentials)
                                                             .build())
                                                 .build();
    return KubernetesClusterConfigDTO.builder().credential(k8sCredentials).build();
  }

  private KubernetesAuthDTO createUserPasswordDTO(K8sUserNamePassword userNamePasswordCredential) {
    KubernetesUserNamePasswordDTO kubernetesUserNamePasswordDTO =
        KubernetesUserNamePasswordDTO.builder()
            .username(userNamePasswordCredential.getUserName())
            .usernameRef(SecretRefHelper.createSecretRef(userNamePasswordCredential.getUserNameRef()))
            .passwordRef(SecretRefHelper.createSecretRef(userNamePasswordCredential.getPasswordRef()))
            .build();
    return KubernetesAuthDTO.builder().authType(USER_PASSWORD).credentials(kubernetesUserNamePasswordDTO).build();
  }

  private KubernetesAuthDTO createClientKeyCertDTO(K8sClientKeyCert k8SClientKeyCert) {
    KubernetesClientKeyCertDTO kubernetesClientKeyCertDTO =
        KubernetesClientKeyCertDTO.builder()
            .clientKeyRef(SecretRefHelper.createSecretRef(k8SClientKeyCert.getClientKeyRef()))
            .clientCertRef(SecretRefHelper.createSecretRef(k8SClientKeyCert.getClientCertRef()))
            .clientKeyPassphraseRef(SecretRefHelper.createSecretRef(k8SClientKeyCert.getClientKeyPassphraseRef()))
            .clientKeyAlgo(k8SClientKeyCert.getClientKeyAlgo())
            .caCertRef(SecretRefHelper.createSecretRef(k8SClientKeyCert.getCaCertRef()))
            .build();
    return KubernetesAuthDTO.builder().authType(CLIENT_KEY_CERT).credentials(kubernetesClientKeyCertDTO).build();
  }

  private KubernetesAuthDTO createServiceAccountDTO(K8sServiceAccount k8SServiceAccount) {
    KubernetesServiceAccountDTO kubernetesServiceAccountDTO =
        KubernetesServiceAccountDTO.builder()
            .serviceAccountTokenRef(new SecretRefData(k8SServiceAccount.getServiceAcccountTokenRef()))
            .build();
    return KubernetesAuthDTO.builder().authType(SERVICE_ACCOUNT).credentials(kubernetesServiceAccountDTO).build();
  }

  private KubernetesAuthDTO createOpenIdConnectDTO(K8sOpenIdConnect k8SOpenIdConnect) {
    KubernetesOpenIdConnectDTO kubernetesOpenIdConnectDTO =
        KubernetesOpenIdConnectDTO.builder()
            .oidcClientIdRef(SecretRefHelper.createSecretRef(k8SOpenIdConnect.getOidcClientIdRef()))
            .oidcIssuerUrl(k8SOpenIdConnect.getOidcIssuerUrl())
            .oidcPasswordRef(SecretRefHelper.createSecretRef(k8SOpenIdConnect.getOidcPasswordRef()))
            .oidcScopes(k8SOpenIdConnect.getOidcScopes())
            .oidcSecretRef(SecretRefHelper.createSecretRef(k8SOpenIdConnect.getOidcSecretRef()))
            .oidcUsername(k8SOpenIdConnect.getOidcUsername())
            .oidcUsernameRef(SecretRefHelper.createSecretRef(k8SOpenIdConnect.getOidcUsernameRef()))
            .build();
    return KubernetesAuthDTO.builder().authType(OPEN_ID_CONNECT).credentials(kubernetesOpenIdConnectDTO).build();
  }

  private K8sUserNamePassword castToUserNamePassowordDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (K8sUserNamePassword) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private K8sClientKeyCert castToClientKeyCertDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (K8sClientKeyCert) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private K8sServiceAccount castToServiceAccountDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (K8sServiceAccount) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }

  private K8sOpenIdConnect castToOpenIdConnectDTO(KubernetesAuth kubernetesAuth) {
    try {
      return (K8sOpenIdConnect) kubernetesAuth;
    } catch (ClassCastException ex) {
      throw new UnexpectedException(
          String.format("The credential type and credentials doesn't match, expected [%s] credentials", USER_PASSWORD),
          ex);
    }
  }
}
