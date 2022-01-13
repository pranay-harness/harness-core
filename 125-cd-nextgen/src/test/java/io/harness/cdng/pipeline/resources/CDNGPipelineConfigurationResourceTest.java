/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.pipeline.resources;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStrategyType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.helpers.CDNGPipelineConfigurationHelper;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.rule.Owner;

import io.fabric8.utils.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CDNGPipelineConfigurationResourceTest extends CategoryTest {
  @Mock CDNGPipelineConfigurationHelper pipelineService;
  CDNGPipelineConfigurationResource cdngPipelineConfigurationResource;
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cdngPipelineConfigurationResource = new CDNGPipelineConfigurationResource(pipelineService);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutionStrategyList() {
    doCallRealMethod().when(pipelineService).getExecutionStrategyList();
    Map<ServiceDefinitionType, List<ExecutionStrategyType>> executionStrategyResponse =
        cdngPipelineConfigurationResource.getExecutionStrategyList().getData();

    assertThat(executionStrategyResponse).isNotNull();
    assertThat(executionStrategyResponse.keySet().size()).isEqualTo(2);
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.KUBERNETES))
        .isEqualTo(Lists.newArrayList(ExecutionStrategyType.ROLLING, ExecutionStrategyType.BLUE_GREEN,
            ExecutionStrategyType.CANARY, ExecutionStrategyType.DEFAULT));

    /*
    Assertions commented as these service definitions are currently not supported
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.NATIVE_HELM))
        .isEqualTo(Lists.newArrayList(ExecutionStrategyType.BASIC));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.PCF))
        .isEqualTo(Lists.newArrayList(
            ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.SSH))
        .isEqualTo(Lists.newArrayList(ExecutionStrategyType.BASIC));
    assertThat(executionStrategyResponse.get(ServiceDefinitionType.ECS))
        .isEqualTo(Lists.newArrayList(
            ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY));
     */
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetServiceDefinitionTypes() {
    when(pipelineService.getServiceDefinitionTypes()).thenReturn(Arrays.asList(ServiceDefinitionType.values()));
    List<ServiceDefinitionType> serviceDefinitionTypes =
        cdngPipelineConfigurationResource.getServiceDefinitionTypes().getData();

    assertThat(serviceDefinitionTypes).isNotNull();
    assertThat(serviceDefinitionTypes.size()).isEqualTo(2);
  }
}
