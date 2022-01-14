package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;

@OwnedBy(PL)
public class LocalDTOToEntity implements ConnectorDTOToEntityMapper<LocalConnectorDTO, LocalConnector> {
  @Override
  public LocalConnector toConnectorEntity(LocalConnectorDTO connectorDTO) {
    return LocalConnector.builder()
        .isDefault(connectorDTO.isDefault())
        .harnessManaged(connectorDTO.isHarnessManaged())
        .build();
  }
}
