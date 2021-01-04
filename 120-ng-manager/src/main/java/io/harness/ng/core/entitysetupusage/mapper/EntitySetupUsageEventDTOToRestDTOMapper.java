package io.harness.ng.core.entitysetupusage.mapper;

import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class EntitySetupUsageEventDTOToRestDTOMapper {
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
}
