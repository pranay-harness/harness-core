package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.core.entities.changeSource.event.HarnessCDChangeEvent;

public class HarnessCDChangeEventTransformer
    extends ChangeEventMetaDataTransformer<HarnessCDChangeEvent, HarnessCDEventMetaData> {
  @Override
  public HarnessCDChangeEvent getEntity(ChangeEventDTO changeEventDTO) {
    HarnessCDEventMetaData metaData = (HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData();
    return HarnessCDChangeEvent.builder()
        .accountId(changeEventDTO.getAccountId())
        .orgIdentifier(changeEventDTO.getOrgIdentifier())
        .projectIdentifier(changeEventDTO.getProjectIdentifier())
        .serviceIdentifier(changeEventDTO.getServiceIdentifier())
        .envIdentifier(changeEventDTO.getEnvIdentifier())
        .eventTime(changeEventDTO.getEventTime())
        .type(changeEventDTO.getType())
        .status(metaData.getStatus())
        .stageId(metaData.getStageId())
        .executionId(metaData.getExecutionId())
        .deploymentStartTime(metaData.getDeploymentStartTime())
        .deploymentEndTime(metaData.getDeploymentEndTime())
        .build();
  }

  @Override
  protected HarnessCDEventMetaData getMetadata(HarnessCDChangeEvent changeEvent) {
    return HarnessCDEventMetaData.builder()
        .deploymentEndTime(changeEvent.getDeploymentEndTime())
        .deploymentStartTime(changeEvent.getDeploymentStartTime())
        .status(changeEvent.getStatus())
        .executionId(changeEvent.getExecutionId())
        .stageId(changeEvent.getStageId())
        .build();
  }
}
