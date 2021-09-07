package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.HarnessCDActivity;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;

import java.time.Instant;

public class HarnessCDChangeEventTransformer
    extends ChangeEventMetaDataTransformer<HarnessCDActivity, HarnessCDEventMetaData> {
  @Override
  public HarnessCDActivity getEntity(ChangeEventDTO changeEventDTO) {
    HarnessCDEventMetaData metaData = (HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData();
    return HarnessCDActivity.builder()
        .accountId(changeEventDTO.getAccountId())
        .activityName(
            "Harness CD - " + changeEventDTO.getServiceIdentifier() + " - " + changeEventDTO.getEnvIdentifier())
        .orgIdentifier(changeEventDTO.getOrgIdentifier())
        .projectIdentifier(changeEventDTO.getProjectIdentifier())
        .serviceIdentifier(changeEventDTO.getServiceIdentifier())
        .environmentIdentifier(changeEventDTO.getEnvIdentifier())
        .eventTime(Instant.ofEpochMilli(changeEventDTO.getEventTime()))
        .changeSourceIdentifier(changeEventDTO.getChangeSourceIdentifier())
        .type(changeEventDTO.getType().getActivityType())
        .deploymentStatus(metaData.getStatus())
        .stageId(metaData.getStageId())
        .executionId(metaData.getExecutionId())
        .activityStartTime(Instant.ofEpochMilli(metaData.getDeploymentStartTime()))
        .activityEndTime(Instant.ofEpochMilli(metaData.getDeploymentEndTime()))
        .build();
  }

  @Override
  protected HarnessCDEventMetaData getMetadata(HarnessCDActivity activity) {
    return HarnessCDEventMetaData.builder()
        .deploymentEndTime(activity.getActivityEndTime().toEpochMilli())
        .deploymentStartTime(activity.getActivityStartTime().toEpochMilli())
        .status(activity.getDeploymentStatus())
        .executionId(activity.getExecutionId())
        .stageId(activity.getStageId())
        .build();
  }
}
