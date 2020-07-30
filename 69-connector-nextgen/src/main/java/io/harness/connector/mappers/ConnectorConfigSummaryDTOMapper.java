package io.harness.connector.mappers;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.entities.Connector;

public interface ConnectorConfigSummaryDTOMapper<T extends Connector> {
  ConnectorConfigSummaryDTO toConnectorConfigSummaryDTO(T connector);
}
