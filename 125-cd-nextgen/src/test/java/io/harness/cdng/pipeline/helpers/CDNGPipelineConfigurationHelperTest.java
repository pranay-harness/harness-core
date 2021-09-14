/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.pipeline.helpers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStrategyType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CDNGPipelineConfigurationHelperTest extends CategoryTest {
  @InjectMocks
  private final CDNGPipelineConfigurationHelper cdngPipelineConfigurationHelper = new CDNGPipelineConfigurationHelper();

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutionStrategyList() {
    Map<ServiceDefinitionType, List<ExecutionStrategyType>> result =
        cdngPipelineConfigurationHelper.getExecutionStrategyList();
    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStepsK8s() {
    StepCategory result = cdngPipelineConfigurationHelper.getSteps(ServiceDefinitionType.KUBERNETES);
    assertThat(result.getName()).isEqualTo(CDNGPipelineConfigurationHelper.LIBRARY);
    assertThat(result.getStepsData()).isEqualTo(new ArrayList<>());
    assertThat(result.getStepCategories().size()).isEqualTo(6);
  }
}
