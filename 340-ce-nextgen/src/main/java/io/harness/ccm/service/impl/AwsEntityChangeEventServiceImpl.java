package io.harness.ccm.service.impl;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.*;

import static java.lang.String.format;

import io.harness.ccm.service.intf.AwsEntityChangeEventService;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;

import com.amazonaws.services.eks.model.NotFoundException;
import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsEntityChangeEventServiceImpl implements AwsEntityChangeEventService {
  @Inject ConnectorResourceClient connectorResourceClient;

  @Override
  public boolean processAWSEntityChangeEvent(EntityChangeDTO entityChangeDTO, String action) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    String projectIdentifier = entityChangeDTO.getProjectIdentifier().getValue();
    String orgIdentifier = entityChangeDTO.getOrgIdentifier().getValue();

    switch (action) {
      case CREATE_ACTION:
        CEAwsConnectorDTO ceAwsConnectorDTO =
            (CEAwsConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
        break;
      case UPDATE_ACTION:
        break;
      case DELETE_ACTION:
        break;
      default:
        log.info("Not processing AWS Event, action: {}, entityChangeDTO: {}", action, entityChangeDTO);
    }
    return false;
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
      throw new NotFoundException(format("Error while getting connector information : [%s]", connectorIdentifierRef));
    }
  }
}
