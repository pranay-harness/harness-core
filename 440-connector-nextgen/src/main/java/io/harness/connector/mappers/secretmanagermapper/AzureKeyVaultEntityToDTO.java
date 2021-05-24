package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.azurekeyvaultconnector.AzureKeyVaultConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConnectorDTO;

@OwnedBy(PL)
public class AzureKeyVaultEntityToDTO
    implements ConnectorEntityToDTOMapper<AzureKeyVaultConnectorDTO, AzureKeyVaultConnector> {
  @Override
  public AzureKeyVaultConnectorDTO createConnectorDTO(AzureKeyVaultConnector connector) {
    return AzureKeyVaultConnectorDTO.builder()
        .isDefault(connector.isDefault())
        .clientId(connector.getClientId())
        .tenantId(connector.getTenantId())
        .vaultName(connector.getVaultName())
        .subscription(connector.getSubscription())
        .azureEnvironmentType(connector.getAzureEnvironmentType())
        .build();
  }
}
