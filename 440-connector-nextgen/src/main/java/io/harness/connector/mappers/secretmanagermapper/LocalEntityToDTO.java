/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
