package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConnectorDTO;

public class GcpKmsEntityToDTO implements ConnectorEntityToDTOMapper<GcpKmsConnector> {
  @Override
  public ConnectorConfigDTO createConnectorDTO(GcpKmsConnector connector) {
    return GcpKmsConnectorDTO.builder()
        .keyName(connector.getKeyName())
        .keyRing(connector.getKeyRing())
        .projectId(connector.getProjectId())
        .region(connector.getRegion())
        .isDefault(connector.isDefault())
        .build();
  }
}
