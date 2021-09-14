/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.sumologicmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.sumologic.SumoLogicConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.sumologic.SumoLogicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(CV)
public class SumoLogicDTOToEntity implements ConnectorDTOToEntityMapper<SumoLogicConnectorDTO, SumoLogicConnector> {
  @Override
  public SumoLogicConnector toConnectorEntity(SumoLogicConnectorDTO connectorDTO) {
    return SumoLogicConnector.builder()
        .url(connectorDTO.getUrl())
        .accessIdRef(SecretRefHelper.getSecretConfigString(connectorDTO.getAccessIdRef()))
        .accessKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getAccessKeyRef()))
        .build();
  }
}
