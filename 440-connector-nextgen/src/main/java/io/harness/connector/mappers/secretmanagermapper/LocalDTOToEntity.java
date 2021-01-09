package io.harness.connector.mappers.secretmanagermapper;

import io.harness.connector.ConnectorCategory;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;

import java.util.Collections;
import java.util.List;

public class LocalDTOToEntity implements ConnectorDTOToEntityMapper<LocalConnectorDTO> {
  @Override
  public Connector toConnectorEntity(LocalConnectorDTO connectorDTO) {
    return LocalConnector.builder()
        .isDefault(connectorDTO.isDefault())
        .harnessManaged(connectorDTO.isHarnessManaged())
        .build();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return Collections.singletonList(ConnectorCategory.SECRET_MANAGER);
  }
}
