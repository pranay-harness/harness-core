package io.harness.connector.mappers.splunkconnectormapper;

import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class SplunkEntityToDTO implements ConnectorEntityToDTOMapper<SplunkConnectorDTO, SplunkConnector> {
  private SecretRefService secretRefService;

  @Override
  public SplunkConnectorDTO createConnectorDTO(SplunkConnector connector) {
    return SplunkConnectorDTO.builder()
        .username(connector.getUsername())
        .passwordRef(secretRefService.createSecretRef(connector.getPasswordRef()))
        .splunkUrl(connector.getSplunkUrl())
        .accountId(connector.getAccountId())
        .build();
  }
}
