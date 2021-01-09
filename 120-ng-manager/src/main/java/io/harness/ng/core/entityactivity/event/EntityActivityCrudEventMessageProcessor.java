package io.harness.ng.core.entityactivity.event;

import static io.harness.ng.core.activityhistory.NGActivityType.CONNECTIVITY_CHECK;

import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entityactivity.EntityActivityCreateDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.entityactivity.connector.ConnectorHeartbeatConsumer;
import io.harness.ng.core.entityactivity.mapper.EntityActivityProtoToRestDTOMapper;
import io.harness.ng.core.event.MessageProcessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntityActivityCrudEventMessageProcessor implements MessageProcessor {
  NGActivityService ngActivityService;
  EntityActivityProtoToRestDTOMapper entityActivityProtoToRestDTOMapper;
  ConnectorHeartbeatConsumer connectorHeartbeatConsumer;

  @Inject
  public EntityActivityCrudEventMessageProcessor(NGActivityService ngActivityService,
      EntityActivityProtoToRestDTOMapper entityActivityProtoToRestDTOMapper,
      ConnectorHeartbeatConsumer connectorHeartbeatConsumer) {
    this.ngActivityService = ngActivityService;
    this.entityActivityProtoToRestDTOMapper = entityActivityProtoToRestDTOMapper;
    this.connectorHeartbeatConsumer = connectorHeartbeatConsumer;
  }

  @Override
  public void processMessage(Message message) {
    String messageId = message.getId();
    log.info("Processing the activity crud event with the id {}", messageId);
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(EventsFrameworkMetadataConstants.ACTION)) {
      switch (metadataMap.get(EventsFrameworkMetadataConstants.ACTION)) {
        case EventsFrameworkMetadataConstants.CREATE_ACTION:
          EntityActivityCreateDTO entityActivityProtoDTO = getEntityActivityCreateDTO(message);
          NGActivityDTO ngActivityDTO = entityActivityProtoToRestDTOMapper.toRestDTO(entityActivityProtoDTO);
          processCreateAction(ngActivityDTO);
          saveConnectivityCheckResultInConnectorRecords(ngActivityDTO);
          return;
        default:
          log.info("Invalid action type: {}", metadataMap.get(EventsFrameworkMetadataConstants.ACTION));
      }
    }
  }

  private void saveConnectivityCheckResultInConnectorRecords(NGActivityDTO ngActivityDTO) {
    if (ngActivityDTO.getType() == CONNECTIVITY_CHECK) {
      connectorHeartbeatConsumer.saveTheConnectivityStatus(ngActivityDTO);
    }
  }

  private void processCreateAction(NGActivityDTO ngActivityDTO) {
    ngActivityService.save(ngActivityDTO);
  }

  private EntityActivityCreateDTO getEntityActivityCreateDTO(Message entitySetupUsageMessage) {
    EntityActivityCreateDTO entityActivityCreateDTO = null;
    try {
      entityActivityCreateDTO = EntityActivityCreateDTO.parseFrom(entitySetupUsageMessage.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityActivityCreateDTO for key {}", entitySetupUsageMessage.getId(), e);
    }
    return entityActivityCreateDTO;
  }
}