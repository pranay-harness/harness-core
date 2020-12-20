package io.harness.ng.core.entityactivity.event;

import static io.harness.EntityCRUDEventsConstants.ACTION_METADATA;
import static io.harness.EntityCRUDEventsConstants.CREATE_ACTION;

import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entityactivity.EntityActivityCreateDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.entityactivity.mapper.EntityActivityProtoToRestDTOMapper;
import io.harness.ng.core.event.ConsumerMessageProcessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntityActivityCrudEventMessageProcessor implements ConsumerMessageProcessor {
  NGActivityService ngActivityService;
  EntityActivityProtoToRestDTOMapper entityActivityProtoToRestDTOMapper;

  @Inject
  public EntityActivityCrudEventMessageProcessor(
      NGActivityService ngActivityService, EntityActivityProtoToRestDTOMapper entityActivityProtoToRestDTOMapper) {
    this.ngActivityService = ngActivityService;
    this.entityActivityProtoToRestDTOMapper = entityActivityProtoToRestDTOMapper;
  }

  @Override
  public void processMessage(Message message) {
    String messageId = message.getId();
    log.info("Processing the activity crud event with the id {}", messageId);
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(ACTION_METADATA)) {
      switch (metadataMap.get(ACTION_METADATA)) {
        case CREATE_ACTION:
          EntityActivityCreateDTO entityActivityProtoDTO = getEntityActivityCreateDTO(message);
          processCreateAction(entityActivityProtoDTO);
          return;
        default:
          log.info("Invalid action type: {}", metadataMap.get(ACTION_METADATA));
      }
    }
  }

  private void processCreateAction(EntityActivityCreateDTO entityActivityProtoDTO) {
    NGActivityDTO ngActivityDTO = entityActivityProtoToRestDTOMapper.toRestDTO(entityActivityProtoDTO);
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