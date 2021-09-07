package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.ABHIJITH;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.HarnessCDActivity;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;
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
  public void testGetEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    HarnessCDActivity harnessCDActivity = harnessCDChangeEventTransformer.getEntity(changeEventDTO);
    verifyEqual(harnessCDActivity, changeEventDTO);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetMetadata() {
    HarnessCDActivity harnessCDActivity = builderFactory.getHarnessCDActivityBuilder().build();
    ChangeEventDTO changeEventDTO = harnessCDChangeEventTransformer.getDTO(harnessCDActivity);
    verifyEqual(harnessCDActivity, changeEventDTO);
  }

  private void verifyEqual(HarnessCDActivity harnessCDActivity, ChangeEventDTO changeEventDTO) {
    Assertions.assertThat(harnessCDActivity.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDActivity.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDActivity.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDActivity.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDActivity.getType()).isEqualTo(changeEventDTO.getType().getActivityType());
    Assertions.assertThat(harnessCDActivity.getActivityEndTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDActivity.getActivityStartTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDActivity.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDActivity.getExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getExecutionId());
  }
}