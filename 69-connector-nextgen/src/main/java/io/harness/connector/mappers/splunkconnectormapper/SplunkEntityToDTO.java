package io.harness.connector.mappers.splunkconnectormapper;

import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

public class SplunkEntityToDTO implements ConnectorEntityToDTOMapper<SplunkConnector> {
  @Override
  public SplunkConnectorDTO createConnectorDTO(SplunkConnector connector) {
    return SplunkConnectorDTO.builder()
        .username(connector.getUsername())
        .passwordReference(connector.getPasswordReference())
        .splunkUrl(connector.getSplunkUrl())
        .accountId(connector.getAccountId())
        .build();
  }
}
