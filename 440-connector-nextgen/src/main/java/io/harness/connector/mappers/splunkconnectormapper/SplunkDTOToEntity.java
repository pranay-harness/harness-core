package io.harness.connector.mappers.splunkconnectormapper;

import io.harness.connector.ConnectorCategory;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class SplunkDTOToEntity implements ConnectorDTOToEntityMapper<SplunkConnectorDTO> {
  @Override
  public SplunkConnector toConnectorEntity(SplunkConnectorDTO connectorDTO) {
    return SplunkConnector.builder()
        .username(connectorDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(connectorDTO.getPasswordRef()))
        .splunkUrl(connectorDTO.getSplunkUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return null;
  }
}
