/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.service.impl;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.remote.client.NGRestUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class K8sConnectorHelper {
  @Inject private ConnectorResourceClient connectorResourceClient;
  @Inject private SecretManagerClientService ngSecretService;

  public List<EncryptedDataDetail> getEncryptionDetail(KubernetesClusterConfigDTO kubernetesClusterConfigDTO,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (kubernetesClusterConfigDTO.getCredential().getKubernetesCredentialType()
        == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesAuthCredential = getKubernetesAuthCredential(
          (KubernetesClusterDetailsDTO) kubernetesClusterConfigDTO.getCredential().getConfig());

      if (kubernetesAuthCredential == null) {
        return null;
      }
      NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                         .accountIdentifier(accountIdentifier)
                                         .orgIdentifier(orgIdentifier)
                                         .projectIdentifier(projectIdentifier)
                                         .build();

      return ngSecretService.getEncryptionDetails(basicNGAccessObject, kubernetesAuthCredential);
    }

    return null;
  }

  private KubernetesAuthCredentialDTO getKubernetesAuthCredential(
      KubernetesClusterDetailsDTO kubernetesClusterConfigDTO) {
    return kubernetesClusterConfigDTO.getAuth().getCredentials();
  }

  public ConnectorConfigDTO getConnectorConfig(
      String connectorIdentifier, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Optional<ConnectorDTO> response = NGRestUtils.getResponse(
        connectorResourceClient.get(connectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier));

    if (response.isPresent()) {
      return response.get().getConnectorInfo().getConnectorConfig();
    }

    throw new InvalidArgumentsException(String.format("connectorIdentifier=[%s] in org=[%s], project=[%s] doesnt exist",
        connectorIdentifier, orgIdentifier, projectIdentifier));
  }
}
