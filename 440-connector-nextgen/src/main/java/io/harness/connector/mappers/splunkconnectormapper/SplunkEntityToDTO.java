package io.harness.connector.mappers.splunkconnectormapper;

import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

public class SplunkEntityToDTO extends ConnectorEntityToDTOMapper<SplunkConnectorDTO, SplunkConnector> {
  @Override
  public SplunkConnectorDTO createConnectorDTO(SplunkConnector connector) {
    return SplunkConnectorDTO.builder()
        .username(connector.getUsername())
        .passwordRef(SecretRefHelper.createSecretRef(connector.getPasswordRef()))
        .splunkUrl(connector.getSplunkUrl())
        .accountId(connector.getAccountId())
        .build();
  }
}
