package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;

import java.util.Collections;

public class VaultDTOToEntity implements ConnectorDTOToEntityMapper<VaultConnectorDTO> {
  @Override
  public Connector toConnectorEntity(VaultConnectorDTO connectorDTO) {
    VaultConnector vaultConnector = VaultConnector.builder()
                                        .accessType(connectorDTO.getAccessType())
                                        .isDefault(connectorDTO.isDefault())
                                        .isReadOnly(connectorDTO.isReadOnly())
                                        .secretEngineName(connectorDTO.getSecretEngineName())
                                        .vaultUrl(connectorDTO.getVaultUrl())
                                        .build();
    vaultConnector.setCategories(Collections.singletonList(ConnectorCategory.SECRET_MANAGER));
    return vaultConnector;
  }
}
