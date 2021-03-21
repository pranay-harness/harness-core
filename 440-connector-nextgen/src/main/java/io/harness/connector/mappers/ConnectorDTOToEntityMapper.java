package io.harness.connector.mappers;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

@OwnedBy(DX)
public interface ConnectorDTOToEntityMapper<D extends ConnectorConfigDTO, B extends Connector> {
  B toConnectorEntity(D connectorDTO);
}
