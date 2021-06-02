package io.harness.connector.mappers.newerlicmapper;

import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class NewRelicDTOToEntity implements ConnectorDTOToEntityMapper<NewRelicConnectorDTO, NewRelicConnector> {
  @Override
  public NewRelicConnector toConnectorEntity(NewRelicConnectorDTO connectorDTO) {
    return NewRelicConnector.builder()
        .apiKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApiKeyRef()))
        .newRelicAccountId(connectorDTO.getNewRelicAccountId())
        .url(connectorDTO.getUrl())
        .build();
  }
}
