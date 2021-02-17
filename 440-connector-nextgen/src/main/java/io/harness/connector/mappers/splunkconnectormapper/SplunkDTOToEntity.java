package io.harness.connector.mappers.splunkconnectormapper;

import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class SplunkDTOToEntity implements ConnectorDTOToEntityMapper<SplunkConnectorDTO, SplunkConnector> {
  private SecretRefService secretRefService;

  @Override
  public SplunkConnector toConnectorEntity(SplunkConnectorDTO connectorDTO, NGAccess ngAccess) {
    return SplunkConnector.builder()
        .username(connectorDTO.getUsername())
        .passwordRef(secretRefService.validateAndGetSecretConfigString(connectorDTO.getPasswordRef(), ngAccess))
        .splunkUrl(connectorDTO.getSplunkUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }
}
