package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public abstract class ConnectorDTOToEntityMapper<D extends ConnectorConfigDTO, B extends Connector> {
  public abstract B toConnectorEntity(D connectorDTO);
}
