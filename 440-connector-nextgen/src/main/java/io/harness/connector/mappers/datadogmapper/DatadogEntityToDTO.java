/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.datadogmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.datadogconnector.DatadogConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(CV)
public class DatadogEntityToDTO implements ConnectorEntityToDTOMapper<DatadogConnectorDTO, DatadogConnector> {
  @Override
  public DatadogConnectorDTO createConnectorDTO(DatadogConnector connector) {
    return DatadogConnectorDTO.builder()
        .url(connector.getUrl())
        .apiKeyRef(SecretRefHelper.createSecretRef(connector.getApiKeyRef()))
        .applicationKeyRef(SecretRefHelper.createSecretRef(connector.getApplicationKeyRef()))
        .build();
  }
}
