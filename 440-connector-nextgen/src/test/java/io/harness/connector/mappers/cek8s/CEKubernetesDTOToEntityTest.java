/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.cek8s;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.cek8s.CEK8sDetails;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEKubernetesDTOToEntityTest extends CategoryTest {
  @InjectMocks CEKubernetesDTOToEntity ceKubernetesDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testToConnectorEntity() {
    final String connectorRef = "connectorRef";
    List<CEFeatures> ceK8sFeatures = Arrays.asList(CEFeatures.VISIBILITY, CEFeatures.OPTIMIZATION);
    final CEK8sDetails cek8sDetails =
        CEK8sDetails.builder().connectorRef(connectorRef).featuresEnabled(ceK8sFeatures).build();

    final CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO =
        CEKubernetesClusterConfigDTO.builder().connectorRef(connectorRef).featuresEnabled(ceK8sFeatures).build();
    assertThat(ceKubernetesDTOToEntity.toConnectorEntity(ceKubernetesClusterConfigDTO)).isEqualTo(cek8sDetails);
  }
}
