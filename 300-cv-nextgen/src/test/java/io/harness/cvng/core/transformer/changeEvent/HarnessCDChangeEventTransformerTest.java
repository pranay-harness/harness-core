/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.HarnessCDEventMetadata;
import io.harness.cvng.client.NextGenService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class HarnessCDChangeEventTransformerTest {
  HarnessCDChangeEventTransformer harnessCDChangeEventTransformer;
  BuilderFactory builderFactory;
  NextGenService nextGenService;

  private String serviceName = "ServiceName";
  private String environmentName = "EnvironmentName";

  @Before
  public void setup() throws IllegalAccessException {
    harnessCDChangeEventTransformer = new HarnessCDChangeEventTransformer();
    nextGenService = Mockito.mock(NextGenService.class);
    FieldUtils.writeField(harnessCDChangeEventTransformer, "nextGenService", nextGenService, true);
    Mockito.when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name(serviceName).build());
    Mockito.when(nextGenService.getEnvironment(any(), any(), any(), any()))
        .thenReturn(EnvironmentResponseDTO.builder().name(environmentName).build());
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    DeploymentActivity harnessCDActivity = harnessCDChangeEventTransformer.getEntity(changeEventDTO);
    verifyEqual(harnessCDActivity, changeEventDTO);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetMetadata() {
    DeploymentActivity harnessCDActivity = builderFactory.getDeploymentActivityBuilder().build();
    ChangeEventDTO changeEventDTO = harnessCDChangeEventTransformer.getDTO(harnessCDActivity);
    verifyEqual(harnessCDActivity, changeEventDTO);
    assertThat(changeEventDTO.getServiceName()).isEqualTo(serviceName);
    assertThat(changeEventDTO.getEnvironmentName()).isEqualTo(environmentName);
    assertThat(harnessCDActivity.getUuid()).isEqualTo(changeEventDTO.getId());
  }

  private void verifyEqual(DeploymentActivity harnessCDActivity, ChangeEventDTO changeEventDTO) {
    assertThat(harnessCDActivity.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    assertThat(harnessCDActivity.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    assertThat(harnessCDActivity.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    assertThat(harnessCDActivity.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    assertThat(harnessCDActivity.getType()).isEqualTo(changeEventDTO.getType().getActivityType());
    assertThat(harnessCDActivity.getActivityEndTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getDeploymentEndTime());
    assertThat(harnessCDActivity.getActivityStartTime().toEpochMilli())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getDeploymentStartTime());
    assertThat(harnessCDActivity.getStageStepId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getStageStepId());
    assertThat(harnessCDActivity.getPlanExecutionId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getPlanExecutionId());
    assertThat(harnessCDActivity.getPipelineId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getPipelineId());
    assertThat(harnessCDActivity.getStageId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getStageId());
    assertThat(harnessCDActivity.getPlanExecutionId())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getPlanExecutionId());
    assertThat(harnessCDActivity.getArtifactType())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getArtifactType());
    assertThat(harnessCDActivity.getArtifactTag())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getArtifactTag());
    assertThat(harnessCDActivity.getDeploymentStatus())
        .isEqualTo(((HarnessCDEventMetadata) changeEventDTO.getMetadata()).getStatus());
  }
}
