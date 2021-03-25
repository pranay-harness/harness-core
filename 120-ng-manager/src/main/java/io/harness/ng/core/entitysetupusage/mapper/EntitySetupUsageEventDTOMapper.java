package io.harness.ng.core.entitysetupusage.mapper;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class EntitySetupUsageEventDTOMapper {
  EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;

  public EntitySetupUsageDTO toRestDTO(EntitySetupUsageCreateDTO setupUsageEventsDTO) {
    EntityDetail referredEntity =
        entityDetailProtoToRestMapper.createEntityDetailDTO(setupUsageEventsDTO.getReferredEntity());
    EntityDetail referredByEntity =
        entityDetailProtoToRestMapper.createEntityDetailDTO(setupUsageEventsDTO.getReferredByEntity());

    return EntitySetupUsageDTO.builder()
        .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
        .createdAt(setupUsageEventsDTO.getCreatedAt())
        .referredByEntity(referredByEntity)
        .referredEntity(referredEntity)
        .build();
  }

  public List<EntitySetupUsage> toEntityDTO(EntitySetupUsageCreateV2DTO setupUsageEventsDTO) {
    final EntityDetail referredByEntity =
        entityDetailProtoToRestMapper.createEntityDetailDTO(setupUsageEventsDTO.getReferredByEntity());
    final List<EntityDetail> referredEntities =
        entityDetailProtoToRestMapper.createEntityDetailsDTO(setupUsageEventsDTO.getReferredEntitiesList());

    return hasNone(referredEntities)
        ? Collections.singletonList(EntitySetupUsage.builder()
                                        .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
                                        .referredByEntity(referredByEntity)
                                        .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
                                        .referredByEntityType(referredByEntity.getType().toString())
                                        .build())
        : referredEntities.stream()
              .map(referredEntity
                  -> EntitySetupUsage.builder()
                         .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
                         .referredByEntity(referredByEntity)
                         .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
                         .referredByEntityType(referredByEntity.getType().toString())
                         .referredEntityFQN(referredEntity.getEntityRef().getFullyQualifiedName())
                         .referredEntityType(referredEntity.getType().toString())
                         .referredEntity(referredEntity)
                         .build())
              .collect(Collectors.toList());
  }
}
