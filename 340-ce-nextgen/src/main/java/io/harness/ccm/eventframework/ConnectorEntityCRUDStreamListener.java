package io.harness.ccm.eventframework;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CE)
@Slf4j
@Singleton
public class ConnectorEntityCRUDStreamListener implements MessageListener {
  @Inject EntityChangeHandler entityChangeHandler;

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (hasRequiredMetadata(metadataMap)) {
        EntityChangeDTO entityChangeDTO;
        try {
          entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
        } catch (InvalidProtocolBufferException e) {
          throw new InvalidRequestException(
              String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
        }
        String action = metadataMap.get(ACTION);
        if (action != null) {
          return processEntityChangeEvent(entityChangeDTO, action, metadataMap.get(CONNECTOR_ENTITY_TYPE));
        }
      }
    }
    return true;
  }

  private static boolean hasRequiredMetadata(Map<String, String> metadataMap) {
    return metadataMap != null && CONNECTOR_ENTITY.equals(metadataMap.get(ENTITY_TYPE))
        && (ConnectorType.KUBERNETES_CLUSTER.getDisplayName().equals(metadataMap.get(CONNECTOR_ENTITY_TYPE))
            || ConnectorType.CE_KUBERNETES_CLUSTER.getDisplayName().equals(metadataMap.get(CONNECTOR_ENTITY_TYPE)));
  }

  private boolean processEntityChangeEvent(EntityChangeDTO entityChangeDTO, String action, String connectorEntityType) {
    log.info("In processEntityChangeEvent {}, {}, {}", entityChangeDTO, action, connectorEntityType);
    switch (action) {
      case CREATE_ACTION:
        entityChangeHandler.handleCreateEvent(entityChangeDTO, connectorEntityType);
        break;
      case UPDATE_ACTION:
        entityChangeHandler.handleUpdateEvent(entityChangeDTO, connectorEntityType);
        break;
      case DELETE_ACTION:
        entityChangeHandler.handleDeleteEvent(entityChangeDTO, connectorEntityType);
        break;
      default:
        log.error("Change Event of type %s, not handled", action);
        return false;
    }
    return true;
  }
}