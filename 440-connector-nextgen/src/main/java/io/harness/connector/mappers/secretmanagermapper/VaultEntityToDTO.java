package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

@OwnedBy(DX)
public class VaultEntityToDTO implements ConnectorEntityToDTOMapper<VaultConnectorDTO, VaultConnector> {
  @Override
  public VaultConnectorDTO createConnectorDTO(VaultConnector connector) {
    return VaultConnectorDTO.builder()
        .isDefault(connector.isDefault())
        .isReadOnly(connector.isReadOnly())
        .vaultUrl(connector.getVaultUrl())
        .secretEngineName(connector.getSecretEngineName())
        .secretEngineVersion(connector.getSecretEngineVersion())
        .renewalIntervalMinutes(connector.getRenewalIntervalMinutes())
        .basePath(connector.getBasePath())
        .secretEngineManuallyConfigured(connector.isSecretEngineManuallyConfigured())
        .appRoleId(connector.getAppRoleId())
        .build();
  }
}
