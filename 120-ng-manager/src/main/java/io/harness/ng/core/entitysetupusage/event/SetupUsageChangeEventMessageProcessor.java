/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.entitysetupusage.event;

import io.harness.EntityType;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.mapper.EntitySetupUsageEventDTOMapper;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.event.MessageProcessor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Deprecated
public class SetupUsageChangeEventMessageProcessor implements MessageProcessor {
  EntitySetupUsageService entitySetupUsageService;
  EntitySetupUsageEventDTOMapper entitySetupUsageEventDTOToRestDTOMapper;

  @Inject
  public SetupUsageChangeEventMessageProcessor(EntitySetupUsageService entitySetupUsageService,
      EntitySetupUsageEventDTOMapper entitySetupUsageEventDTOToRestDTOMapper) {
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
        EntityType.valueOf(deleteRequestDTO.getReferredEntityType().name()), deleteRequestDTO.getReferredByEntityFQN(),
        EntityType.valueOf(deleteRequestDTO.getReferredByEntityType().name()));
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
