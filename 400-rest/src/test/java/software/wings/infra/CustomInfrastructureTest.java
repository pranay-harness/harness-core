/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.NameValuePair;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomInfrastructureTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testFieldKeyValMapProvider() {
    assertThat(CustomInfrastructure.builder().build()).isInstanceOf(FieldKeyValMapProvider.class);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetInfraMapping() {
    final CustomInfrastructure customInfrastructure =
        CustomInfrastructure.builder()
            .customDeploymentName("my-deploymentype")
            .infraVariables(Arrays.asList(NameValuePair.builder().name("name").value("value").build()))
            .deploymentTypeTemplateVersion("1.1")
            .build();

    final CustomInfrastructureMapping infraMapping =
        (CustomInfrastructureMapping) customInfrastructure.getInfraMapping();

    assertThat(infraMapping.getInfraVariables())
        .containsExactly(NameValuePair.builder().name("name").value("value").build());
    assertThat(infraMapping.getInfraMappingType()).isEqualTo(InfrastructureMappingType.CUSTOM.name());
    assertThat(infraMapping.getComputeProviderSettingId()).isEqualTo(CustomInfrastructure.DUMMY_CLOUD_PROVIDER);
    assertThat(customInfrastructure.getCloudProviderId()).isEqualTo(CustomInfrastructure.DUMMY_CLOUD_PROVIDER);
    assertThat(customInfrastructure.getDeploymentTypeTemplateVersion()).isEqualTo("1.1");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetInfrastructureType() {
    assertThat(CustomInfrastructure.builder().build().getInfrastructureType())
        .isEqualTo(InfrastructureMappingType.CUSTOM.name());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetMappingClass() {
    assertThat(CustomInfrastructure.builder().build().getMappingClass()).isEqualTo(CustomInfrastructureMapping.class);
  }
}
