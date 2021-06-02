package io.harness.connector.mappers.datadogmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.datadogconnector.DatadogConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(CV)
public class DatadogDTOToEntity implements ConnectorDTOToEntityMapper<DatadogConnectorDTO, DatadogConnector> {
  @Override
  public DatadogConnector toConnectorEntity(DatadogConnectorDTO connectorDTO) {
    return DatadogConnector.builder()
        .url(connectorDTO.getUrl())
        .apiKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApiKeyRef()))
        .applicationKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApplicationKeyRef()))
        .build();
  }
}
