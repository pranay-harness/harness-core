package io.harness.ng.core.activityhistory.mapper;

import com.google.inject.Singleton;

import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.entity.ConnectivityCheckDetail;
import io.harness.ng.core.activityhistory.entity.EntityUsageActivityDetail;
import io.harness.ng.core.activityhistory.entity.NGActivity;
import io.harness.ng.core.activityhistory.entity.NGActivity.NGActivityBuilder;
import io.harness.utils.FullyQualifiedIdentifierHelper;

@Singleton
public class NGActivityDTOToEntityMapper {
  public NGActivity toActivityHistoryEntity(NGActivityDTO activityHistoryDTO) {
    NGActivity activityEntity = null;
    switch (activityHistoryDTO.getType()) {
      case CONNECTIVITY_CHECK:
        activityEntity = getActivityForConnectivityCheck(activityHistoryDTO);
        break;
      case ENTITY_USAGE:
        activityEntity = getActivityForEntityUsage(activityHistoryDTO);
        break;
      default:
        throw new UnknownEnumTypeException("NGActivity", String.valueOf(activityHistoryDTO.getType()));
    }
    return activityEntity;
  }

  private NGActivity getActivityForConnectivityCheck(NGActivityDTO activityDTO) {
    NGActivityBuilder builder = ConnectivityCheckDetail.builder();
    populateCommonActivityProperties(builder, activityDTO);
    return builder.build();
  }

  private void populateCommonActivityProperties(NGActivityBuilder activityBuilder, NGActivityDTO activityDTO) {
    activityBuilder.accountIdentifier(activityDTO.getAccountIdentifier())
        .referredEntityOrgIdentifier(activityDTO.getReferredEntityOrgIdentifier())
        .referredEntityProjectIdentifier(activityDTO.getReferredEntityProjectIdentifier())
        .referredEntityIdentifier(activityDTO.getReferredEntityIdentifier())
        .activityStatus(String.valueOf(activityDTO.getActivityStatus()))
        .activityTime(activityDTO.getActivityTime())
        .description(activityDTO.getDescription())
        .errorMessage(activityDTO.getErrorMessage())
        .referredEntityFQN(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
            activityDTO.getAccountIdentifier(), activityDTO.getReferredEntityOrgIdentifier(),
            activityDTO.getReferredEntityProjectIdentifier(), activityDTO.getReferredEntityIdentifier()))
        .referredEntityType(String.valueOf(activityDTO.getReferredEntityType()))
        .type(String.valueOf(activityDTO.getType()))
        .referredEntityScope(String.valueOf(activityDTO.getReferredEntityScope()));
  }

  private NGActivity getActivityForEntityUsage(NGActivityDTO activityDTO) {
    EntityUsageActivityDetailDTO entityUsageDTO = (EntityUsageActivityDetailDTO) activityDTO.getDetail();
    NGActivityBuilder builder =
        EntityUsageActivityDetail.builder()
            .referredByEntityOrgIdentifier(entityUsageDTO.getReferredByEntityOrgIdentifier())
            .referredByEntityProjectIdentifier(entityUsageDTO.getReferredByEntityProjectIdentifier())
            .referredByEntityIdentifier(entityUsageDTO.getReferredByEntityIdentifier())
            .referredByEntityScope(String.valueOf(entityUsageDTO.getReferredByEntityScope()))
            .referredByEntityFQN(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
                activityDTO.getAccountIdentifier(), entityUsageDTO.getReferredByEntityOrgIdentifier(),
                entityUsageDTO.getReferredByEntityProjectIdentifier(), entityUsageDTO.getReferredByEntityIdentifier()))
            .referredByEntityType(String.valueOf(entityUsageDTO.getReferredByEntityType()));
    populateCommonActivityProperties(builder, activityDTO);
    return builder.build();
  }
}
