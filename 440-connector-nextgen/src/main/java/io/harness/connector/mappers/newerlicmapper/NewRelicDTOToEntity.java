/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
