/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.dynatracemapper;

import io.harness.connector.entities.embedded.dynatraceconnector.DynatraceConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class DynatraceDTOToEntity implements ConnectorDTOToEntityMapper<DynatraceConnectorDTO, DynatraceConnector> {
  @Override
  public DynatraceConnector toConnectorEntity(DynatraceConnectorDTO connectorDTO) {
    return DynatraceConnector.builder()
        .url(connectorDTO.getUrl())
        .apiTokenRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApiTokenRef()))
        .build();
  }
}
