package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

public class VaultConnectorSummaryDTOMapper implements ConnectorConfigSummaryDTOMapper<VaultConnector> {
  @Override
  public ConnectorConfigSummaryDTO toConnectorConfigSummaryDTO(VaultConnector connector) {
    return VaultConnectorSummaryDTO.builder()
        .vaultUrl(connector.getVaultUrl())
        .secretEngineName(connector.getSecretEngineName())
        .accessType(connector.getAccessType())
        .isDefault(connector.isDefault())
        .isReadOnly(connector.isReadOnly())
        .renewalIntervalHours(connector.getRenewalIntervalHours())
        .build();
  }
}
