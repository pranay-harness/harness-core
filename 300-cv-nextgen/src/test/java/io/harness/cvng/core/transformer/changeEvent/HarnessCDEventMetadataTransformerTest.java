package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.core.entities.changeSource.event.HarnessCDChangeEvent;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class HarnessCDEventMetadataTransformerTest {
  HarnessCDEventMetadataTransformer harnessCDEventMetadataTransformer;

  BuilderFactory builderFactory;

  @Before
  public void setup() {
    harnessCDEventMetadataTransformer = new HarnessCDEventMetadataTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  public void getEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    HarnessCDChangeEvent harnessCDChangeEvent = harnessCDEventMetadataTransformer.getEntity(changeEventDTO);
    Assertions.assertThat(harnessCDChangeEvent.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDChangeEvent.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getEventTime()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDChangeEvent.getType()).isEqualTo(changeEventDTO.getType());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentEndTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentStartTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDChangeEvent.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDChangeEvent.getExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getExecutionId());
  }

  @Test
  public void getMetadata() {
    HarnessCDChangeEvent harnessCDChangeEvent = builderFactory.getHarnessCDChangeEventBuilder().build();
    ChangeEventDTO changeEventDTO = harnessCDEventMetadataTransformer.getDTO(harnessCDChangeEvent);
    Assertions.assertThat(harnessCDChangeEvent.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDChangeEvent.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getEventTime()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDChangeEvent.getType()).isEqualTo(changeEventDTO.getType());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentEndTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentStartTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDChangeEvent.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDChangeEvent.getExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getExecutionId());
  }
}