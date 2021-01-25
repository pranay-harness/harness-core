package io.harness.ng.core.entitysetupusage.event;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.CONNECTORS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.SECRETS;

import io.harness.EntityType;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entitysetupusage.DeleteSetupUsageDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.ng.core.entitysetupusage.mapper.EntitySetupUsageEventDTOMapper;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.event.EventProtoToEntityHelper;
import io.harness.ng.core.event.MessageListener;

import com.amazonaws.services.eks.model.InvalidRequestException;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SetupUsageChangeEventMessageListener implements MessageListener {
  EntitySetupUsageService entitySetupUsageService;
  EntitySetupUsageEventDTOMapper entitySetupUsageEventDTOToRestDTOMapper;
  final Set<EntityTypeProtoEnum> entityTypesSupportedByNGCore = Sets.newHashSet(SECRETS, CONNECTORS);

  @Inject
  public SetupUsageChangeEventMessageListener(EntitySetupUsageService entitySetupUsageService,
      EntitySetupUsageEventDTOMapper entitySetupUsageEventDTOToRestDTOMapper) {
    this.entitySetupUsageService = entitySetupUsageService;
    this.entitySetupUsageEventDTOToRestDTOMapper = entitySetupUsageEventDTOToRestDTOMapper;
  }

  @Override
  public boolean handleMessage(Message message) {
    final String messageId = message.getId();
    log.info("Processing the setup usage crud event with the id {}", messageId);
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (!metadataMap.containsKey(REFERRED_ENTITY_TYPE) || !handledByNgCore(metadataMap.get(REFERRED_ENTITY_TYPE))) {
      return false;
    }
    final EntityType entityTypeFromProto = EventProtoToEntityHelper.getEntityTypeFromProto(
        EntityTypeProtoEnum.valueOf(metadataMap.get(REFERRED_ENTITY_TYPE)));
    if (metadataMap.containsKey(EventsFrameworkMetadataConstants.ACTION)) {
      switch (metadataMap.get(EventsFrameworkMetadataConstants.ACTION)) {
        case EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION:
          final EntitySetupUsageCreateV2DTO setupUsageCreateDTO = getEntitySetupUsageCreateDTO(message);
          return processCreateAction(setupUsageCreateDTO, entityTypeFromProto);
        case EventsFrameworkMetadataConstants.DELETE_ACTION:
          final DeleteSetupUsageDTO deleteRequestDTO = getEntitySetupUsageDeleteDTO(message);
          return processDeleteAction(deleteRequestDTO, entityTypeFromProto);
        default:
          log.info("Invalid action type: {}", metadataMap.get(EventsFrameworkMetadataConstants.ACTION));
      }
    }
    return false;
  }

  private boolean handledByNgCore(String entityTypeProtoEnum) {
    return entityTypesSupportedByNGCore.contains(EntityTypeProtoEnum.valueOf(entityTypeProtoEnum));
  }

  private Boolean processDeleteAction(DeleteSetupUsageDTO deleteRequestDTO, EntityType entityTypeFromProto) {
    if (deleteRequestDTO == null) {
      return false;
    }
    if (deleteRequestDTO.getReferredEntityType() == EntityTypeProtoEnum.valueOf(entityTypeFromProto.name())) {
      throw new InvalidRequestException(
          String.format("Delete action for wrong entity: [%s] type published with wrong meta data map: [%s]",
              deleteRequestDTO.getReferredEntityType(), entityTypeFromProto));
    }
    return entitySetupUsageService.delete(deleteRequestDTO.getAccountIdentifier(),
        deleteRequestDTO.getReferredEntityFQN(), EntityType.valueOf(deleteRequestDTO.getReferredEntityType().name()),
        deleteRequestDTO.getReferredByEntityFQN(), entityTypeFromProto);
  }

  private Boolean processCreateAction(EntitySetupUsageCreateV2DTO setupUsageCreateDTO, EntityType entityType) {
    if (setupUsageCreateDTO == null) {
      return false;
    }
    final List<EntitySetupUsage> entitySetupUsages =
        entitySetupUsageEventDTOToRestDTOMapper.toEntityDTO(setupUsageCreateDTO);
    return entitySetupUsageService.saveNew(entitySetupUsages, entityType,
        setupUsageCreateDTO.getDeleteOldReferredByRecords(), setupUsageCreateDTO.getAccountIdentifier());
  }

  private EntitySetupUsageCreateV2DTO getEntitySetupUsageCreateDTO(Message entitySetupUsageMessage) {
    EntitySetupUsageCreateV2DTO entitySetupUsageCreateDTO = null;
    try {
      entitySetupUsageCreateDTO = EntitySetupUsageCreateV2DTO.parseFrom(entitySetupUsageMessage.getMessage().getData());
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