package io.harness.connector.mappers;

import io.harness.connector.ConnectorCategory;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import java.util.List;

public interface ConnectorDTOToEntityMapper<T extends ConnectorConfigDTO> {
  Connector toConnectorEntity(T connectorDTO);

  List<ConnectorCategory> getConnectorCategory();
}
