package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.PagerDutyActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.PagerDutyEventMetaData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PagerdutyChangeEventTransformerTest {
  PagerDutyChangeEventTransformer pagerDutyChangeEventTransformer;

  BuilderFactory builderFactory;

  @Before
  public void setup() {
    pagerDutyChangeEventTransformer = new PagerDutyChangeEventTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getPagerDutyChangeEventDTOBuilder().build();
    PagerDutyActivity pagerDutyActivity = pagerDutyChangeEventTransformer.getEntity(changeEventDTO);
    verifyEqual(pagerDutyActivity, changeEventDTO);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetMetadata() {
    PagerDutyActivity pagerDutyActivity = builderFactory.getPagerDutyActivityBuilder().build();
    ChangeEventDTO changeEventDTO = pagerDutyChangeEventTransformer.getDTO(pagerDutyActivity);
    verifyEqual(pagerDutyActivity, changeEventDTO);
  }

  private void verifyEqual(PagerDutyActivity pagerDutyActivity, ChangeEventDTO changeEventDTO) {
    PagerDutyEventMetaData pagerDutyEventMetaData = (PagerDutyEventMetaData) changeEventDTO.getChangeEventMetaData();
    assertThat(pagerDutyActivity.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    assertThat(pagerDutyActivity.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    assertThat(pagerDutyActivity.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    assertThat(pagerDutyActivity.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    assertThat(pagerDutyActivity.getType()).isEqualTo(changeEventDTO.getType().getActivityType());
    assertThat(pagerDutyActivity.getActivityEndTime()).isNull();
    assertThat(pagerDutyActivity.getActivityStartTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    assertThat(pagerDutyActivity.getEventId()).isEqualTo(pagerDutyEventMetaData.getEventId());
    assertThat(pagerDutyActivity.getPagerDutyUrl()).isEqualTo(pagerDutyEventMetaData.getPagerDutyUrl());
    assertThat(pagerDutyActivity.getActivityName()).isEqualTo(pagerDutyEventMetaData.getTitle());
  }
}
