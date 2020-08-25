package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

public class LocalConnectorSummaryDTOMapper implements ConnectorConfigSummaryDTOMapper<LocalConnector> {
  @Override
  public ConnectorConfigSummaryDTO toConnectorConfigSummaryDTO(LocalConnector connector) {
    return LocalConnectorSummaryDTO.builder().isDefault(connector.isDefault()).build();
  }
}
