package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

public class GcpKmsConnectorSummaryDTOMapper implements ConnectorConfigSummaryDTOMapper<GcpKmsConnector> {
  @Override
  public ConnectorConfigSummaryDTO toConnectorConfigSummaryDTO(GcpKmsConnector connector) {
    return GcpKmsConnectorSummaryDTO.builder()
        .projectId(connector.getProjectId())
        .region(connector.getRegion())
        .keyRing(connector.getKeyRing())
        .keyName(connector.getKeyName())
        .isDefault(connector.isDefault())
        .build();
  }
}
