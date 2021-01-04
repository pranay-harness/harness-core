package io.harness.connector.mappers.secretmanagermapper;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

import java.util.Collections;
import java.util.List;

public class VaultDTOToEntity implements ConnectorDTOToEntityMapper<VaultConnectorDTO> {
  @Override
  public Connector toConnectorEntity(VaultConnectorDTO connectorDTO) {
    return VaultConnector.builder()
        .accessType(connectorDTO.getAccessType())
        .isDefault(connectorDTO.isDefault())
        .isReadOnly(connectorDTO.isReadOnly())
        .secretEngineName(connectorDTO.getSecretEngineName())
        .vaultUrl(connectorDTO.getVaultUrl())
        .secretEngineVersion(connectorDTO.getSecretEngineVersion())
        .renewalIntervalHours(connectorDTO.getRenewIntervalHours())
        .appRoleId(connectorDTO.getAppRoleId())
        .basePath(connectorDTO.getBasePath())
        .build();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return Collections.singletonList(ConnectorCategory.SECRET_MANAGER);
  }
}
