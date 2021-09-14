/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.dynatracemapper;

import io.harness.connector.entities.embedded.dynatraceconnector.DynatraceConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class DynatraceEntityToDTO implements ConnectorEntityToDTOMapper<DynatraceConnectorDTO, DynatraceConnector> {
  @Override
  public DynatraceConnectorDTO createConnectorDTO(DynatraceConnector connector) {
    return DynatraceConnectorDTO.builder()
        .url(connector.getUrl())
        .apiTokenRef(SecretRefHelper.createSecretRef(connector.getApiTokenRef()))
        .build();
  }
}
