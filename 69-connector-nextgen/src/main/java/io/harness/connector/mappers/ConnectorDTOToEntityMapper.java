package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public interface ConnectorDTOToEntityMapper<T extends ConnectorConfigDTO> {
  Connector toConnectorEntity(T connectorDTO);
}
