package io.harness.connector.mappers;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.ng.core.NGAccess;

public interface ConnectorDTOToEntityMapper<D extends ConnectorConfigDTO, B extends Connector> {
  B toConnectorEntity(D connectorDTO, NGAccess ngAccess);
}
