/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.localconnector.LocalConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;

@OwnedBy(PL)
public class LocalEntityToDTO implements ConnectorEntityToDTOMapper<LocalConnectorDTO, LocalConnector> {
  @Override
  public LocalConnectorDTO createConnectorDTO(LocalConnector connector) {
    LocalConnectorDTO localConnectorDTO = LocalConnectorDTO.builder().isDefault(connector.isDefault()).build();
    localConnectorDTO.setHarnessManaged(Boolean.TRUE.equals(connector.getHarnessManaged()));
    return localConnectorDTO;
  }
}
