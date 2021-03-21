package io.harness.connector.mappers.newerlicmapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(DX)
public class NewRelicEntityToDTO implements ConnectorEntityToDTOMapper<NewRelicConnectorDTO, NewRelicConnector> {
  @Override
  public NewRelicConnectorDTO createConnectorDTO(NewRelicConnector connector) {
    return NewRelicConnectorDTO.builder()
        .newRelicAccountId(connector.getNewRelicAccountId())
        .apiKeyRef(SecretRefHelper.createSecretRef(connector.getApiKeyRef()))
        .url(connector.getUrl())
        .build();
  }
}
