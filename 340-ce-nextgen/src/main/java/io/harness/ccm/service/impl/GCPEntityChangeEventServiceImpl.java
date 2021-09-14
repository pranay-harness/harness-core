/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.service.impl;

import static java.lang.String.format;

import io.harness.ccm.commons.dao.CEGcpServiceAccountDao;
import io.harness.ccm.service.intf.GCPEntityChangeEventService;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;

import com.google.inject.Inject;
import java.util.Optional;

public class GCPEntityChangeEventServiceImpl implements GCPEntityChangeEventService {
  @Inject ConnectorResourceClient connectorResourceClient;
  @Inject CEGcpServiceAccountDao ceGcpServiceAccountDao;

  @Override
  public boolean processGCPEntityCreateEvent(EntityChangeDTO entityChangeDTO) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();

    GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
        (GcpCloudCostConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
    ceGcpServiceAccountDao.setProjectId(
        gcpCloudCostConnectorDTO.getServiceAccountEmail(), gcpCloudCostConnectorDTO.getProjectId(), accountIdentifier);
    return true;
  }

  public ConnectorInfoDTO getConnectorConfigDTO(String accountIdentifier, String connectorIdentifierRef) {
    try {
      Optional<ConnectorDTO> connectorDTO =
          NGRestUtils.getResponse(connectorResourceClient.get(connectorIdentifierRef, accountIdentifier, null, null));

      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(format("Connector not found for identifier : [%s]", connectorIdentifierRef));
      }

      return connectorDTO.get().getConnectorInfo();
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Error while getting connector information : [%s]", connectorIdentifierRef));
    }
  }
}
