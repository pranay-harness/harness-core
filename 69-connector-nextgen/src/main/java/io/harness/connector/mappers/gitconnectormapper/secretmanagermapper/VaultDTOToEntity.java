package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.secretmanagerclient.dto.VaultConnectorDTO;

public class VaultDTOToEntity implements ConnectorDTOToEntityMapper<VaultConnectorDTO> {
  @Override
  public Connector toConnectorEntity(VaultConnectorDTO connectorDTO) {
    return VaultConnector.builder()
        .accessType(connectorDTO.getAccessType())
        .isDefault(connectorDTO.isDefault())
        .isReadOnly(connectorDTO.isReadOnly())
        .secretEngineName(connectorDTO.getSecretEngineName())
        .vaultUrl(connectorDTO.getVaultUrl())
        .build();
  }
}
