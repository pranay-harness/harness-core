package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.HarnessCDActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.HarnessCDEventMetadata;
import io.harness.rule.Owner;

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
    assertThat(harnessCDActivity.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    assertThat(harnessCDActivity.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    assertThat(harnessCDActivity.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    assertThat(harnessCDActivity.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    assertThat(harnessCDActivity.getType()).isEqualTo(changeEventDTO.getType().getActivityType());
    assertThat(harnessCDActivity.getActivityEndTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    assertThat(harnessCDActivity.getActivityStartTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    assertThat(harnessCDActivity.getStageStepId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getStageStepId());
    assertThat(harnessCDActivity.getPlanExecutionId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getPlanExecutionId());
    assertThat(harnessCDActivity.getPipelineId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getPipelineId());
    assertThat(harnessCDActivity.getStageId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getStageId());
    assertThat(harnessCDActivity.getPlanExecutionId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getPlanExecutionId());
    assertThat(harnessCDActivity.getArtifactType())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getArtifactType());
    assertThat(harnessCDActivity.getArtifactTag())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getArtifactTag());
    assertThat(harnessCDActivity.getDeploymentStatus())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getChangeEventMetaData()).getStatus());
  }
}