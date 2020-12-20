package io.harness.ng.core.entityactivity.mapper;

import io.harness.eventsframework.schemas.entityactivity.EntityActivityCreateDTO;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.ActivityDetail;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class EntityActivityProtoToRestDTOMapper {
  EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;

  public NGActivityDTO toRestDTO(EntityActivityCreateDTO entityActivityProtoDTO) {
    return NGActivityDTO.builder()
        .accountIdentifier(entityActivityProtoDTO.getAccountIdentifier())
        .type(NGActivityType.valueOf(entityActivityProtoDTO.getType()))
        .activityStatus(NGActivityStatus.valueOf(entityActivityProtoDTO.getStatus()))
        .activityTime(entityActivityProtoDTO.getActivityTime())
        .description(entityActivityProtoDTO.getDescription())
        .errorMessage(entityActivityProtoDTO.getErrorMessage())
        .referredEntity(entityDetailProtoToRestMapper.createEntityDetailDTO(entityActivityProtoDTO.getReferredEntity()))
        .detail(createEntityActivityDetailDTO(entityActivityProtoDTO.getType(), entityActivityProtoDTO))
        .build();
  }

  private ActivityDetail createEntityActivityDetailDTO(String type, EntityActivityCreateDTO entityActivityProtoDTO) {
    if (NGActivityType.CONNECTIVITY_CHECK.toString().equals(type)) {
      return ConnectivityCheckActivityDetailDTO.builder().build();
    } else if (NGActivityType.ENTITY_USAGE.toString().equals(type)) {
      EntityActivityCreateDTO.EntityUsageActivityDetailProtoDTO entityUsageActivityDetailProtoDTO =
          entityActivityProtoDTO.getEntityUsageDetail();
      return EntityUsageActivityDetailDTO.builder()
          .referredByEntity(entityDetailProtoToRestMapper.createEntityDetailDTO(
              entityUsageActivityDetailProtoDTO.getReferredByEntity()))
          .activityStatusMessage(entityUsageActivityDetailProtoDTO.getActivityStatusMessage())
          .build();
    } else {
      return null;
    }
  }
}
