/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.PagerDutyActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.PagerDutyEventMetaData;
import io.harness.cvng.client.NextGenService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class PagerdutyChangeEventTransformerTest {
  PagerDutyChangeEventTransformer pagerDutyChangeEventTransformer;
  BuilderFactory builderFactory;
  NextGenService nextGenService;

  @Before
  public void setup() throws IllegalAccessException {
    pagerDutyChangeEventTransformer = new PagerDutyChangeEventTransformer();
    nextGenService = Mockito.mock(NextGenService.class);
    FieldUtils.writeField(pagerDutyChangeEventTransformer, "nextGenService", nextGenService, true);
    Mockito.when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("serviceName").build());
    Mockito.when(nextGenService.getEnvironment(any(), any(), any(), any()))
        .thenReturn(EnvironmentResponseDTO.builder().name("environmentName").build());
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
    PagerDutyEventMetaData pagerDutyEventMetaData = (PagerDutyEventMetaData) changeEventDTO.getMetadata();
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
