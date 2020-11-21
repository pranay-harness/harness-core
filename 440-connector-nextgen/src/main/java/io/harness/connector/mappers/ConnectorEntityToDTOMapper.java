package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public interface ConnectorEntityToDTOMapper<T extends Connector> {
  ConnectorConfigDTO createConnectorDTO(T connector);
}
