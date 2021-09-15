package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.PagerDutyActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.PagerDutyEventMetaData;

import java.time.Instant;

public class PagerDutyChangeEventTransformer
    extends ChangeEventMetaDataTransformer<PagerDutyActivity, PagerDutyEventMetaData> {
  @Override
  public PagerDutyActivity getEntity(ChangeEventDTO changeEventDTO) {
    PagerDutyEventMetaData pagerDutyEventMetaData = (PagerDutyEventMetaData) changeEventDTO.getChangeEventMetaData();
    return PagerDutyActivity.builder()
        .accountId(changeEventDTO.getAccountId())
        .activityName(pagerDutyEventMetaData.getTitle())
        .orgIdentifier(changeEventDTO.getOrgIdentifier())
        .projectIdentifier(changeEventDTO.getProjectIdentifier())
        .serviceIdentifier(changeEventDTO.getServiceIdentifier())
        .environmentIdentifier(changeEventDTO.getEnvIdentifier())
        .eventTime(Instant.ofEpochMilli(changeEventDTO.getEventTime()))
        .activityStartTime(Instant.ofEpochMilli(changeEventDTO.getEventTime()))
        .changeSourceIdentifier(changeEventDTO.getChangeSourceIdentifier())
        .type(changeEventDTO.getType().getActivityType())
        .pagerDutyUrl(pagerDutyEventMetaData.getPagerDutyUrl())
        .eventId(pagerDutyEventMetaData.getEventId())
        .build();
  }

  @Override
  protected PagerDutyEventMetaData getMetadata(PagerDutyActivity activity) {
    return PagerDutyEventMetaData.builder()
        .pagerDutyUrl(activity.getPagerDutyUrl())
        .eventId(activity.getEventId())
        .title(activity.getActivityName())
        .build();
  }
}
