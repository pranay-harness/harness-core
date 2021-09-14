/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.argo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.argo.ArgoConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.argo.ArgoConnectorDTO;

@OwnedBy(CDP)
public class ArgoDTOToEntity implements ConnectorDTOToEntityMapper<ArgoConnectorDTO, ArgoConnector> {
  @Override
  public ArgoConnector toConnectorEntity(ArgoConnectorDTO connectorDTO) {
    return ArgoConnector.builder().adapterUrl(connectorDTO.getAdapterUrl()).build();
  }
}
