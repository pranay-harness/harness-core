/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.HarnessCDActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.HarnessCDEventMetadata;

import java.time.Instant;

public class HarnessCDChangeEventTransformer
    extends ChangeEventMetaDataTransformer<HarnessCDActivity, HarnessCDEventMetadata> {
  @Override
  public HarnessCDActivity getEntity(ChangeEventDTO changeEventDTO) {
    HarnessCDEventMetadata metaData = (HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData();
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
        .planExecutionId(metaData.getPlanExecutionId())
        .stageStepId(metaData.getStageStepId())
        .pipelineId(metaData.getPipelineId())
        .stageId(metaData.getStageId())
        .planExecutionId(metaData.getPlanExecutionId())
        .activityStartTime(Instant.ofEpochMilli(metaData.getDeploymentStartTime()))
        .activityEndTime(Instant.ofEpochMilli(metaData.getDeploymentEndTime()))
        .artifactTag(metaData.getArtifactTag())
        .artifactType(metaData.getArtifactType())
        .build();
  }

  @Override
  protected HarnessCDEventMetadata getMetadata(HarnessCDActivity activity) {
    return HarnessCDEventMetadata.builder()
        .deploymentEndTime(activity.getActivityEndTime().toEpochMilli())
        .deploymentStartTime(activity.getActivityStartTime().toEpochMilli())
        .stageId(activity.getStageId())
        .status(activity.getDeploymentStatus())
        .planExecutionId(activity.getPlanExecutionId())
        .stageStepId(activity.getStageStepId())
        .pipelineId(activity.getPipelineId())
        .stageId(activity.getStageId())
        .artifactTag(activity.getArtifactTag())
        .artifactType(activity.getArtifactType())
        .build();
  }
}
