package io.harness.connector.mappers.secretmanagermapper;

import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;

public class GcpKmsEntityToDTO implements ConnectorEntityToDTOMapper<GcpKmsConnectorDTO, GcpKmsConnector> {
  @Override
  public GcpKmsConnectorDTO createConnectorDTO(GcpKmsConnector connector) {
    GcpKmsConnectorDTO gcpKmsConnectorDTO = GcpKmsConnectorDTO.builder()
                                                .keyName(connector.getKeyName())
                                                .keyRing(connector.getKeyRing())
                                                .projectId(connector.getProjectId())
                                                .region(connector.getRegion())
                                                .isDefault(connector.isDefault())
                                                .build();
    gcpKmsConnectorDTO.setHarnessManaged(Boolean.TRUE.equals(connector.getHarnessManaged()));
    return gcpKmsConnectorDTO;
  }
}
