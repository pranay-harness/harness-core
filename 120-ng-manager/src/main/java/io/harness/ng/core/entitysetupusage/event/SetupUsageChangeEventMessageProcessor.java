package io.harness.ng.core.entitysetupusage.event;

import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.mapper.EntitySetupUsageEventDTOToRestDTOMapper;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.event.MessageProcessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SetupUsageChangeEventMessageProcessor implements MessageProcessor {
  EntitySetupUsageService entitySetupUsageService;
  EntitySetupUsageEventDTOToRestDTOMapper entitySetupUsageEventDTOToRestDTOMapper;

  @Inject
  public SetupUsageChangeEventMessageProcessor(EntitySetupUsageService entitySetupUsageService,
      EntitySetupUsageEventDTOToRestDTOMapper entitySetupUsageEventDTOToRestDTOMapper) {
    this.entitySetupUsageService = entitySetupUsageService;
    this.entitySetupUsageEventDTOToRestDTOMapper = entitySetupUsageEventDTOToRestDTOMapper;
  }

  @Override
  public boolean processMessage(Message message) {
    String messageId = message.getId();
    log.info("Processing the setup usage crud event with the id {}", messageId);
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap.containsKey(EventsFrameworkMetadataConstants.ACTION)) {
      switch (metadataMap.get(EventsFrameworkMetadataConstants.ACTION)) {
        case EventsFrameworkMetadataConstants.CREATE_ACTION:
          EntitySetupUsageCreateDTO setupUsageCreateDTO = getEntitySetupUsageCreateDTO(message);
          processCreateAction(setupUsageCreateDTO);
          return true;
        case EventsFrameworkMetadataConstants.DELETE_ACTION:
          DeleteSetupUsageDTO deleteRequestDTO = getEntitySetupUsageDeleteDTO(message);
          processDeleteAction(deleteRequestDTO);
          return true;
        default:
          log.info("Invalid action type: {}", metadataMap.get(EventsFrameworkMetadataConstants.ACTION));
      }
    }
    return true;
  }

  private void processDeleteAction(DeleteSetupUsageDTO deleteRequestDTO) {
    entitySetupUsageService.delete(deleteRequestDTO.getAccountIdentifier(), deleteRequestDTO.getReferredEntityFQN(),
        deleteRequestDTO.getReferredByEntityFQN());
  }

  private void processCreateAction(EntitySetupUsageCreateDTO setupUsageCreateDTO) {
    EntitySetupUsageDTO entitySetupUsageDTO = entitySetupUsageEventDTOToRestDTOMapper.toRestDTO(setupUsageCreateDTO);
    entitySetupUsageService.save(entitySetupUsageDTO);
  }

  private EntitySetupUsageCreateDTO getEntitySetupUsageCreateDTO(Message entitySetupUsageMessage) {
    EntitySetupUsageCreateDTO entitySetupUsageCreateDTO = null;
    try {
      entitySetupUsageCreateDTO = EntitySetupUsageCreateDTO.parseFrom(entitySetupUsageMessage.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntitySetupUsageCreateDTO   for key {}", entitySetupUsageMessage.getId(), e);
    }
    return entitySetupUsageCreateDTO;
  }

  private DeleteSetupUsageDTO getEntitySetupUsageDeleteDTO(Message entityDeleteMessage) {
    DeleteSetupUsageDTO deleteRequestDTO = null;
    try {
      deleteRequestDTO = DeleteSetupUsageDTO.parseFrom(entityDeleteMessage.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking DeleteSetupUsageDTO for key {}", entityDeleteMessage.getId(), e);
    }
    return deleteRequestDTO;
  }
}