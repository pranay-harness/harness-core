package io.harness.connector.mappers.secretmanagermapper;

import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

public class VaultDTOToEntity extends ConnectorDTOToEntityMapper<VaultConnectorDTO, VaultConnector> {
  @Override
  public VaultConnector toConnectorEntity(VaultConnectorDTO connectorDTO) {
    return VaultConnector.builder()
        .accessType(connectorDTO.getAccessType())
        .isDefault(connectorDTO.isDefault())
        .isReadOnly(connectorDTO.isReadOnly())
        .secretEngineName(connectorDTO.getSecretEngineName())
        .vaultUrl(connectorDTO.getVaultUrl())
        .secretEngineVersion(connectorDTO.getSecretEngineVersion())
        .secretEngineManuallyConfigured(connectorDTO.isSecretEngineManuallyConfigured())
        .renewalIntervalMinutes(connectorDTO.getRenewalIntervalMinutes())
        .appRoleId(connectorDTO.getAppRoleId())
        .basePath(connectorDTO.getBasePath())
        .build();
  }
}
