package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(DX)
public class SplunkEntityToDTO implements ConnectorEntityToDTOMapper<SplunkConnectorDTO, SplunkConnector> {
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
