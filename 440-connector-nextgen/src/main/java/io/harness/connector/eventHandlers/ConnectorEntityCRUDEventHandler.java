/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.eventHandlers;

import static io.harness.ConnectorConstants.CONNECTOR_DECORATOR_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@OwnedBy(DX)
@Singleton
@Slf4j
public class ConnectorEntityCRUDEventHandler {
  ConnectorService connectorService;

  @Inject
  public ConnectorEntityCRUDEventHandler(@Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService) {
    this.connectorService = connectorService;
  }

  public boolean deleteAssociatedConnectors(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<ConnectorResponseDTO> connectorResponseDTOS =
        fetchAllConnectorInGivenScope(accountIdentifier, orgIdentifier, projectIdentifier);
    List<String> connectorIdentifiers = connectorResponseDTOS.stream()
                                            .map(ConnectorResponseDTO::getConnector)
                                            .map(ConnectorInfoDTO::getIdentifier)
                                            .collect(Collectors.toList());
    if (!connectorIdentifiers.isEmpty()) {
      connectorService.deleteBatch(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifiers);
    }
    return true;
  }

  private List<ConnectorResponseDTO> fetchAllConnectorInGivenScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Page<ConnectorResponseDTO> pagedConnectorList = null;
    List<ConnectorResponseDTO> connectorList = new ArrayList<>();
    do {
      pagedConnectorList = connectorService.list(pagedConnectorList == null ? 0 : pagedConnectorList.getNumber() + 1,
          10, accountIdentifier, null, orgIdentifier, projectIdentifier, null, null, false, true);
      connectorList.addAll(pagedConnectorList.stream().collect(Collectors.toList()));
    } while (pagedConnectorList.hasNext());
    return connectorList;
  }
}
