package io.harness.connector.mappers.splunkconnectormapper;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

@Singleton
public class SplunkDTOToEntity implements ConnectorDTOToEntityMapper<SplunkConnectorDTO> {
  @Override
  public SplunkConnector toConnectorEntity(SplunkConnectorDTO connectorDTO) {
    return SplunkConnector.builder()
        .username(connectorDTO.getUsername())
        .password(connectorDTO.getPassword())
        .passwordReference(connectorDTO.getPasswordReference())
        .splunkUrl(connectorDTO.getSplunkUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }
}
