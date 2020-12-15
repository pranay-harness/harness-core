package io.harness.connector.mappers.secretmanagermapper;

import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

public class VaultEntityToDTO implements ConnectorEntityToDTOMapper<VaultConnector> {
  @Override
  public ConnectorConfigDTO createConnectorDTO(VaultConnector connector) {
    return VaultConnectorDTO.builder()
        .isDefault(connector.isDefault())
        .isReadOnly(connector.isReadOnly())
        .vaultUrl(connector.getVaultUrl())
        .secretEngineName(connector.getSecretEngineName())
        .secretEngineVersion(connector.getSecretEngineVersion())
        .renewIntervalHours(connector.getRenewalIntervalHours())
        .basePath(connector.getBasePath())
        .appRoleId(connector.getAppRoleId())
        .build();
  }
}
