package io.harness.ng.core.entitysetupusage.mapper;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.schemas.entitysetupusage.EntityDetailWithSetupUsageDetailProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetail;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class EntitySetupUsageEventDTOMapper {
  EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;
  SetupUsageDetailProtoToRestMapper setupUsageDetailProtoToRestMapper;

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
    List<EntitySetupUsage> setupUsages = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(referredEntities)) {
      referredEntities.forEach(referredEntity
          -> setupUsages.add(EntitySetupUsage.builder()
                                 .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
                                 .referredByEntity(referredByEntity)
                                 .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
                                 .referredByEntityType(referredByEntity.getType().toString())
                                 .referredEntityFQN(referredEntity.getEntityRef().getFullyQualifiedName())
                                 .referredEntityType(referredEntity.getType().toString())
                                 .referredEntity(referredEntity)
                                 .build()));
    }

    List<EntityDetailWithSetupUsageDetailProtoDTO> entityDetailWithSetupUsageDetailProtoDTOs =
        setupUsageEventsDTO.getReferredEntityWithSetupUsageDetailList();
    if (EmptyPredicate.isNotEmpty(entityDetailWithSetupUsageDetailProtoDTOs)) {
      entityDetailWithSetupUsageDetailProtoDTOs.forEach(entityDetailWithSetupUsageDetailProtoDTO -> {
        SetupUsageDetail setupUsageDetail =
            setupUsageDetailProtoToRestMapper.toRestDTO(entityDetailWithSetupUsageDetailProtoDTO);
        EntityDetail referredEntity = entityDetailProtoToRestMapper.createEntityDetailDTO(
            entityDetailWithSetupUsageDetailProtoDTO.getReferredEntity());
        setupUsages.add(EntitySetupUsage.builder()
                            .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
                            .referredByEntity(referredByEntity)
                            .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
                            .referredByEntityType(referredByEntity.getType().toString())
                            .referredEntityFQN(referredEntity.getEntityRef().getFullyQualifiedName())
                            .referredEntityType(referredEntity.getType().toString())
                            .referredEntity(referredEntity)
                            .detail(setupUsageDetail)
                            .build());
      });
    }

    if (EmptyPredicate.isEmpty(setupUsages)) {
      setupUsages.add(EntitySetupUsage.builder()
                          .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
                          .referredByEntity(referredByEntity)
                          .referredByEntityFQN(referredByEntity.getEntityRef().getFullyQualifiedName())
                          .referredByEntityType(referredByEntity.getType().toString())
                          .build());
    }

    return setupUsages;
  }
}
