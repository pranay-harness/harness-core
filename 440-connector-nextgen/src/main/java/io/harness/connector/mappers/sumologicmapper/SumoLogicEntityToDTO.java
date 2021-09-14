/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.sumologicmapper;

import io.harness.connector.entities.embedded.sumologic.SumoLogicConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.sumologic.SumoLogicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class SumoLogicEntityToDTO implements ConnectorEntityToDTOMapper<SumoLogicConnectorDTO, SumoLogicConnector> {
  @Override
  public SumoLogicConnectorDTO createConnectorDTO(SumoLogicConnector connector) {
    return SumoLogicConnectorDTO.builder()
        .url(connector.getUrl())
        .accessIdRef(SecretRefHelper.createSecretRef(connector.getAccessIdRef()))
        .accessKeyRef(SecretRefHelper.createSecretRef(connector.getAccessKeyRef()))
        .build();
  }
}
