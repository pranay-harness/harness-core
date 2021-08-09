package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public interface ConnectorEntityToDTOMapper<D extends ConnectorConfigDTO, B extends Connector> {
  D createConnectorDTO(B connector);
}
