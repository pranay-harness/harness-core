package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public abstract class ConnectorEntityToDTOMapper<D extends ConnectorConfigDTO, B extends Connector> {
  public abstract D createConnectorDTO(B connector);
}
