package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.gcpkmsconnector.GcpKmsConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;

import java.util.Collections;

public class GcpKmsDTOToEntity implements ConnectorDTOToEntityMapper<GcpKmsConnectorDTO> {
  @Override
  public Connector toConnectorEntity(GcpKmsConnectorDTO connectorDTO) {
    GcpKmsConnector gcpKmsConnector = GcpKmsConnector.builder()
                                          .projectId(connectorDTO.getProjectId())
                                          .region(connectorDTO.getRegion())
                                          .keyRing(connectorDTO.getKeyRing())
                                          .keyName(connectorDTO.getKeyName())
                                          .isDefault(connectorDTO.isDefault())
                                          .build();
    gcpKmsConnector.setCategories(Collections.singletonList(ConnectorCategory.SECRET_MANAGER));
    return gcpKmsConnector;
  }
}
