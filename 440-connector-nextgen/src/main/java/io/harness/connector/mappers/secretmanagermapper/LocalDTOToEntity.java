package io.harness.connector.mappers.secretmanagermapper;

import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.ng.core.NGAccess;

public class LocalDTOToEntity implements ConnectorDTOToEntityMapper<LocalConnectorDTO, LocalConnector> {
  @Override
  public LocalConnector toConnectorEntity(LocalConnectorDTO connectorDTO, NGAccess ngAccess) {
    return LocalConnector.builder()
        .isDefault(connectorDTO.isDefault())
        .harnessManaged(connectorDTO.isHarnessManaged())
        .build();
  }
}
