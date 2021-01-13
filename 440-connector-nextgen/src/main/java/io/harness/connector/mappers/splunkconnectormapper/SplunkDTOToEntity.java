package io.harness.connector.mappers.splunkconnectormapper;

import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
public class SplunkDTOToEntity extends ConnectorDTOToEntityMapper<SplunkConnectorDTO, SplunkConnector> {
  @Override
  public SplunkConnector toConnectorEntity(SplunkConnectorDTO connectorDTO) {
    return SplunkConnector.builder()
        .username(connectorDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(connectorDTO.getPasswordRef()))
        .splunkUrl(connectorDTO.getSplunkUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }
}
