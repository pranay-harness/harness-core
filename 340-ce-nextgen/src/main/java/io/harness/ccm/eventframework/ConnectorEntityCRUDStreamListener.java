package io.harness.ccm.eventframework;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.*;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.service.intf.AwsEntityChangeEventService;
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
  @Inject AwsEntityChangeEventService awsEntityChangeEventService;
  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (hasRequiredMetadata(metadataMap)) {
        EntityChangeDTO entityChangeDTO = getEntityChangeDTO(message);
        String action = metadataMap.get(ACTION);
        if (action != null) {
          return processK8sEntityChangeEvent(entityChangeDTO, action);
        }
      }

      if (isCEAWSEvent(metadataMap)) {
        EntityChangeDTO entityChangeDTO = getEntityChangeDTO(message);
        String action = metadataMap.get(ACTION);
        if (action != null) {
          return awsEntityChangeEventService.processAWSEntityChangeEvent(entityChangeDTO, action);
        }
      }
    }
    return true;
  }

  private EntityChangeDTO getEntityChangeDTO(Message message) {
    EntityChangeDTO entityChangeDTO;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    return entityChangeDTO;
  }

  private static boolean hasRequiredMetadata(Map<String, String> metadataMap) {
    return metadataMap != null && CONNECTOR_ENTITY.equals(metadataMap.get(ENTITY_TYPE))
        && (ConnectorType.KUBERNETES_CLUSTER.getDisplayName().equals(metadataMap.get(CONNECTOR_ENTITY_TYPE))
            || ConnectorType.CE_KUBERNETES_CLUSTER.getDisplayName().equals(metadataMap.get(CONNECTOR_ENTITY_TYPE)));
  }

  private static boolean isCEAWSEvent(Map<String, String> metadataMap) {
    return metadataMap != null && CONNECTOR_ENTITY.equals(metadataMap.get(ENTITY_TYPE))
        && ConnectorType.CE_AWS.getDisplayName().equals(metadataMap.get(CONNECTOR_ENTITY_TYPE));
  }

  private boolean processK8sEntityChangeEvent(EntityChangeDTO entityChangeDTO, String action) {
    // TODO (Rohit): Implement Handling Logic for Managing Cluster Record/ PT Lifecycle
    log.info("In processEntityChangeEvent {}, {}", entityChangeDTO, action);
    return true;
  }
}