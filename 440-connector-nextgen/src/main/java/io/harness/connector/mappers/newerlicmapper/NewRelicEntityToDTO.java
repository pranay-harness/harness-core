/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.newerlicmapper;

import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

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
