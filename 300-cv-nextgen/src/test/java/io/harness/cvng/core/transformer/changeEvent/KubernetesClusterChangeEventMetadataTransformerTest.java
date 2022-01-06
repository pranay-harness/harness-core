/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.KubernetesClusterActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;
import io.harness.cvng.client.NextGenService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class KubernetesClusterChangeEventMetadataTransformerTest {
  KubernetesClusterChangeEventMetadataTransformer transformer;
  BuilderFactory builderFactory;
  NextGenService nextGenService;

  @Before
  public void setup() throws IllegalAccessException {
    transformer = new KubernetesClusterChangeEventMetadataTransformer();
    nextGenService = Mockito.mock(NextGenService.class);
    FieldUtils.writeField(transformer, "nextGenService", nextGenService, true);
    Mockito.when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("serviceName").build());
    Mockito.when(nextGenService.getEnvironment(any(), any(), any(), any()))
        .thenReturn(EnvironmentResponseDTO.builder().name("environmentName").build());
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getKubernetesClusterChangeEventDTOBuilder().build();
    KubernetesClusterActivity activity = transformer.getEntity(changeEventDTO);
    verifyEqual(activity, changeEventDTO);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetMetadata() {
    KubernetesClusterActivity activity = builderFactory.getKubernetesClusterActivityBuilder().build();
    ChangeEventDTO changeEventDTO = transformer.getDTO(activity);
    verifyEqual(activity, changeEventDTO);
  }

  private void verifyEqual(KubernetesClusterActivity activity, ChangeEventDTO changeEventDTO) {
    KubernetesChangeEventMetadata metadata = (KubernetesChangeEventMetadata) changeEventDTO.getMetadata();
    assertThat(activity.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    assertThat(activity.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    assertThat(activity.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    assertThat(activity.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    assertThat(activity.getType()).isEqualTo(changeEventDTO.getType().getActivityType());
    assertThat(activity.getAction().name()).isEqualTo(metadata.getAction().name());
    assertThat(activity.getResourceType().name()).isEqualTo(metadata.getResourceType().name());

    assertThat(activity.getNamespace()).isEqualTo(metadata.getNamespace());
    assertThat(activity.getWorkload()).isEqualTo(metadata.getWorkload());
    assertThat(activity.getOldYaml()).isEqualTo(metadata.getOldYaml());
    assertThat(activity.getNewYaml()).isEqualTo(metadata.getNewYaml());
  }
}
