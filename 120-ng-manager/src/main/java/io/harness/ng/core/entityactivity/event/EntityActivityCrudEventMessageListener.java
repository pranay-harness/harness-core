package io.harness.ng.core.entityactivity.event;

import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entityactivity.EntityActivityCreateDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.entityactivity.EntityActivityEventHandler;
import io.harness.ng.core.entityactivity.mapper.EntityActivityProtoToRestDTOMapper;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class EntityActivityCrudEventMessageListener implements MessageListener {
  NGActivityService ngActivityService;
  EntityActivityProtoToRestDTOMapper entityActivityProtoToRestDTOMapper;
  EntityActivityEventHandler entityActivityEventHandler;

  @Inject
  public EntityActivityCrudEventMessageListener(NGActivityService ngActivityService,
      EntityActivityProtoToRestDTOMapper entityActivityProtoToRestDTOMapper,
      EntityActivityEventHandler entityActivityEventHandler) {
    this.ngActivityService = ngActivityService;
    this.entityActivityProtoToRestDTOMapper = entityActivityProtoToRestDTOMapper;
    this.entityActivityEventHandler = entityActivityEventHandler;
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

  @Override
  public boolean handleMessage(Message message) {
    String messageId = message.getId();
    log.info("Processing the activity crud event with the id {}", messageId);
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    try {
      if (metadataMap.containsKey(EventsFrameworkMetadataConstants.ACTION)) {
        switch (metadataMap.get(EventsFrameworkMetadataConstants.ACTION)) {
          case EventsFrameworkMetadataConstants.CREATE_ACTION:
            EntityActivityCreateDTO entityActivityProtoDTO = getEntityActivityCreateDTO(message);
            NGActivityDTO ngActivityDTO = entityActivityProtoToRestDTOMapper.toRestDTO(entityActivityProtoDTO);
            processCreateAction(ngActivityDTO);
            entityActivityEventHandler.updateActivityResultInEntity(ngActivityDTO);
            return true;
          default:
            log.info("Invalid action type: {}", metadataMap.get(EventsFrameworkMetadataConstants.ACTION));
        }
      }
      log.info("Completed processing the activity crud event with the id {}", messageId);
      return true;
    } catch (Exception ex) {
      log.info("Error processing the activity crud event with the id {}", messageId, ex);
    }
    return false;
  }
}