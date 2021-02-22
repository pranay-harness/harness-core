package io.harness.pms.pipeline;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PipelineSetupUsageHelper {
  @Inject @Named(EventsFrameworkConstants.SETUP_USAGE) private Producer eventProducer;
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  public void publishSetupUsageEvent(PipelineEntity pipelineEntity, List<EntityDetailProtoDTO> referredEntities)
      throws ProducerShutdownException {
    if (EmptyPredicate.isEmpty(referredEntities)) {
      return;
    }
    EntityDetailProtoDTO pipelineDetails =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(pipelineEntity.getAccountId(),
                pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
                pipelineEntity.getIdentifier()))
            .setType(EntityTypeProtoEnum.PIPELINES)
            .setName(pipelineEntity.getName())
            .build();

    Map<String, List<EntityDetailProtoDTO>> referredEntityTypeToReferredEntities = new HashMap<>();
    for (EntityDetailProtoDTO entityDetailProtoDTO : referredEntities) {
      List<EntityDetailProtoDTO> entityDetailProtoDTOS =
          referredEntityTypeToReferredEntities.getOrDefault(entityDetailProtoDTO.getType().name(), new ArrayList<>());
      entityDetailProtoDTOS.add(entityDetailProtoDTO);
      referredEntityTypeToReferredEntities.put(entityDetailProtoDTO.getType().name(), entityDetailProtoDTOS);
    }

    for (Map.Entry<String, List<EntityDetailProtoDTO>> entry : referredEntityTypeToReferredEntities.entrySet()) {
      EntitySetupUsageCreateV2DTO entityReferenceDTO = EntitySetupUsageCreateV2DTO.newBuilder()
                                                           .setAccountIdentifier(pipelineEntity.getAccountId())
                                                           .setReferredByEntity(pipelineDetails)
                                                           .addAllReferredEntities(entry.getValue())
                                                           .setDeleteOldReferredByRecords(true)
                                                           .build();
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                  EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, entry.getKey(),
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
              .setData(entityReferenceDTO.toByteString())
              .build());
    }
  }

  public void deleteSetupUsagesForGivenPipeline(PipelineEntity pipelineEntity) throws ProducerShutdownException {
    EntityDetailProtoDTO pipelineDetails =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(pipelineEntity.getAccountId(),
                pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
                pipelineEntity.getIdentifier()))
            .setType(EntityTypeProtoEnum.PIPELINES)
            .setName(pipelineEntity.getName())
            .build();

    EntitySetupUsageCreateV2DTO entityReferenceDTO = EntitySetupUsageCreateV2DTO.newBuilder()
                                                         .setAccountIdentifier(pipelineEntity.getAccountId())
                                                         .setReferredByEntity(pipelineDetails)
                                                         .addAllReferredEntities(new ArrayList<>())
                                                         .setDeleteOldReferredByRecords(true)
                                                         .build();
    // Send Events for all refferredEntitiesType so as to delete them
    for (EntityTypeProtoEnum protoEnum : EntityTypeProtoEnum.values()) {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                  EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, protoEnum.name(),
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
              .setData(entityReferenceDTO.toByteString())
              .build());
    }
  }
}
