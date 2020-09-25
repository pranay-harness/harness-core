package io.harness.ng.core.activityhistory.mapper;

import com.google.inject.Singleton;

import io.harness.EntityType;
import io.harness.encryption.Scope;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.activityhistory.dto.ActivityDetail;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckSummaryDTO;
import io.harness.ng.core.activityhistory.dto.EntityUsageActivityDetailDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.entity.EntityUsageActivityDetail;
import io.harness.ng.core.activityhistory.entity.NGActivity;

@Singleton
public class NGActivityEntityToDTOMapper {
  public NGActivityDTO writeDTO(NGActivity activity) {
    ActivityDetail activityDetail = getActivityDetail(activity);
    return NGActivityDTO.builder()
        .accountIdentifier(activity.getAccountIdentifier())
        .referredEntityOrgIdentifier(activity.getReferredEntityOrgIdentifier())
        .referredEntityProjectIdentifier(activity.getReferredEntityProjectIdentifier())
        .referredEntityIdentifier(activity.getReferredEntityIdentifier())
        .referredEntityScope(Scope.valueOf(activity.getReferredEntityScope()))
        .activityStatus(NGActivityStatus.valueOf(activity.getActivityStatus()))
        .activityTime(activity.getActivityTime())
        .description(activity.getDescription())
        .type(NGActivityType.valueOf(activity.getType()))
        .detail(activityDetail)
        .errorMessage(activity.getErrorMessage())
        .referredEntityType(EntityType.valueOf(activity.getReferredEntityType()))
        .build();
  }

  private ActivityDetail getActivityDetail(NGActivity activity) {
    switch (NGActivityType.valueOf(activity.getType())) {
      case CONNECTIVITY_CHECK:
        return ConnectivityCheckSummaryDTO.builder().build();
      case ENTITY_USAGE:
        EntityUsageActivityDetail entityUsageActivity = (EntityUsageActivityDetail) activity;
        return EntityUsageActivityDetailDTO.builder()
            .referredByEntityOrgIdentifier(entityUsageActivity.getReferredByEntityOrgIdentifier())
            .referredByEntityProjectIdentifier(entityUsageActivity.getReferredByEntityProjectIdentifier())
            .referredByEntityIdentifier(entityUsageActivity.getReferredByEntityIdentifier())
            .referredByEntityScope(Scope.valueOf(entityUsageActivity.getReferredByEntityScope()))
            .referredByEntityType(EntityType.valueOf(entityUsageActivity.getReferredByEntityType()))
            .build();
      default:
        throw new UnknownEnumTypeException("NGActivity", String.valueOf(activity.getType()));
    }
  }
}
