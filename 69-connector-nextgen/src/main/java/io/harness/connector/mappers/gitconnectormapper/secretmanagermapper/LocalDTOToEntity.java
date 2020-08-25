package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.secretmanagerclient.dto.LocalConnectorDTO;

import java.util.Collections;

public class LocalDTOToEntity implements ConnectorDTOToEntityMapper<LocalConnectorDTO> {
  @Override
  public Connector toConnectorEntity(LocalConnectorDTO connectorDTO) {
    LocalConnector localConnector = LocalConnector.builder().isDefault(connectorDTO.isDefault()).build();
    localConnector.setCategories(Collections.singletonList(ConnectorCategory.SECRET_MANAGER));
    return localConnector;
  }
}
