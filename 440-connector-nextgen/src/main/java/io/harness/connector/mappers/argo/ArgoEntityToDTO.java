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
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.argo.ArgoConnectorDTO;

@OwnedBy(CDP)
public class ArgoEntityToDTO implements ConnectorEntityToDTOMapper<ArgoConnectorDTO, ArgoConnector> {
  @Override
  public ArgoConnectorDTO createConnectorDTO(ArgoConnector connector) {
    return ArgoConnectorDTO.builder().adapterUrl(connector.getAdapterUrl()).build();
  }
}
