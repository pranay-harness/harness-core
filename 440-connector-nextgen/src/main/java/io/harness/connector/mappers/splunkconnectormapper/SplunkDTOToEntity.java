package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class SplunkDTOToEntity implements ConnectorDTOToEntityMapper<SplunkConnectorDTO, SplunkConnector> {
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
