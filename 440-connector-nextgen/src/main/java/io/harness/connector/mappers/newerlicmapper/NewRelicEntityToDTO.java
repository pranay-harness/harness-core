package io.harness.connector.mappers.newerlicmapper;

import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class NewRelicEntityToDTO implements ConnectorEntityToDTOMapper<NewRelicConnectorDTO, NewRelicConnector> {
  @Override
  public NewRelicConnectorDTO createConnectorDTO(NewRelicConnector connector) {
    return NewRelicConnectorDTO.builder()
        .url(connector.getUrl())
        .accountId(connector.getAccountId())
        .apiKeyRef(SecretRefHelper.createSecretRef(connector.getApiKeyRef()))
        .build();
  }
}
