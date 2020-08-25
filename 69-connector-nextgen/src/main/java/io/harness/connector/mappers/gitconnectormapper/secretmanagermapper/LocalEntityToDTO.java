package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.secretmanagerclient.dto.LocalConnectorDTO;

public class LocalEntityToDTO implements ConnectorEntityToDTOMapper<LocalConnector> {
  @Override
  public ConnectorConfigDTO createConnectorDTO(LocalConnector connector) {
    return LocalConnectorDTO.builder().isDefault(connector.isDefault()).build();
  }
}
