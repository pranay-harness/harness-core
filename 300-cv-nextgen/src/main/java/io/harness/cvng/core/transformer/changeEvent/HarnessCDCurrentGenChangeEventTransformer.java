package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.HarnessCDCurrentGenActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.HarnessCDCurrentGenEventMetadata;

import java.time.Instant;

public class HarnessCDCurrentGenChangeEventTransformer
    extends ChangeEventMetaDataTransformer<HarnessCDCurrentGenActivity, HarnessCDCurrentGenEventMetadata> {
  @Override
  public HarnessCDCurrentGenActivity getEntity(ChangeEventDTO changeEventDTO) {
    HarnessCDCurrentGenEventMetadata metaData = (HarnessCDCurrentGenEventMetadata) changeEventDTO.getMetadata();
    return HarnessCDCurrentGenActivity.builder()
        .accountId(changeEventDTO.getAccountId())
        .activityName(
            "Deployment of " + changeEventDTO.getServiceIdentifier() + " in " + changeEventDTO.getEnvIdentifier())
        .orgIdentifier(changeEventDTO.getOrgIdentifier())
        .projectIdentifier(changeEventDTO.getProjectIdentifier())
        .serviceIdentifier(changeEventDTO.getServiceIdentifier())
        .environmentIdentifier(changeEventDTO.getEnvIdentifier())
        .eventTime(Instant.ofEpochMilli(changeEventDTO.getEventTime()))
        .changeSourceIdentifier(changeEventDTO.getChangeSourceIdentifier())
        .type(changeEventDTO.getType().getActivityType())
        .serviceId(metaData.getServiceId())
        .appId(metaData.getAppId())
        .environmentId(metaData.getEnvironmentId())
        .workflowId(metaData.getWorkflowId())
        .workflowExecutionId(metaData.getWorkflowExecutionId())
        .workflowStartTime(Instant.ofEpochMilli(metaData.getWorkflowStartTime()))
        .workflowEndTime(Instant.ofEpochMilli(metaData.getWorkflowEndTime()))
        .name(metaData.getName())
        .build();
  }

  @Override
  protected HarnessCDCurrentGenEventMetadata getMetadata(HarnessCDCurrentGenActivity activity) {
    return HarnessCDCurrentGenEventMetadata.builder()
        .accountId(activity.getAccountId())
        .appId(activity.getAppId())
        .serviceId(activity.getServiceId())
        .environmentId(activity.getServiceId())
        .workflowId(activity.getWorkflowId())
        .workflowExecutionId(activity.getWorkflowExecutionId())
        .workflowStartTime(activity.getWorkflowStartTime().toEpochMilli())
        .workflowEndTime(activity.getWorkflowEndTime().toEpochMilli())
        .name(activity.getName())
        .build();
  }
}
