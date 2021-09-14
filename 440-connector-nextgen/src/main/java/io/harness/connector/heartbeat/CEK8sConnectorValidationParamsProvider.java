/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.heartbeat;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public class CEK8sConnectorValidationParamsProvider extends K8sConnectorValidationParamsProvider {
  @Inject @Named("defaultConnectorService") ConnectorService connectorService;

  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorInfoDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final String scopedConnectorIdentifier =
        ((CEKubernetesClusterConfigDTO) connectorInfoDTO.getConnectorConfig()).getConnectorRef();

    Optional<ConnectorInfoDTO> k8sConnectorInfoDTO =
        getReferencedConnectorConfig(scopedConnectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
    if (!k8sConnectorInfoDTO.isPresent()) {
      throw new InvalidRequestException(
          String.format("There does not exist a K8sCluster Cloud Provider connector with kubernetesConnectorRef=[%s]",
              scopedConnectorIdentifier));
    }
    return super.getConnectorValidationParams(
        k8sConnectorInfoDTO.get(), connectorName, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private Optional<ConnectorInfoDTO> getReferencedConnectorConfig(@NotNull String scopedConnectorIdentifier,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(
        scopedConnectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
    Optional<ConnectorResponseDTO> connectorResponseDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());
    return connectorResponseDTO.map(ConnectorResponseDTO::getConnector);
  }
}
