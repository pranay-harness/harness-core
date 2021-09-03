package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.ABHIJITH;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.core.entities.changeSource.event.HarnessCDChangeEvent;
import io.harness.rule.Owner;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HarnessCDChangeEventTransformerTest {
  HarnessCDChangeEventTransformer harnessCDChangeEventTransformer;

  BuilderFactory builderFactory;

  @Before
  public void setup() {
    harnessCDChangeEventTransformer = new HarnessCDChangeEventTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void getEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    HarnessCDChangeEvent harnessCDChangeEvent = harnessCDChangeEventTransformer.getEntity(changeEventDTO);
    Assertions.assertThat(harnessCDChangeEvent.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDChangeEvent.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDChangeEvent.getType()).isEqualTo(changeEventDTO.getType());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentEndTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentStartTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDChangeEvent.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDChangeEvent.getExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getExecutionId());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void getMetadata() {
    HarnessCDChangeEvent harnessCDChangeEvent = builderFactory.getHarnessCDChangeEventBuilder().build();
    ChangeEventDTO changeEventDTO = harnessCDChangeEventTransformer.getDTO(harnessCDChangeEvent);
    Assertions.assertThat(harnessCDChangeEvent.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDChangeEvent.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDChangeEvent.getType()).isEqualTo(changeEventDTO.getType());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentEndTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentStartTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDChangeEvent.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDChangeEvent.getExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getExecutionId());
  }
}