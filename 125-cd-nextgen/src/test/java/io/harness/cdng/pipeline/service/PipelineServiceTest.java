package io.harness.cdng.pipeline.service;

import static io.harness.rule.OwnerRule.SAHIL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStrategyType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PipelineServiceTest extends CDNGBaseTest {
  @InjectMocks private PipelineService pipelineService = new PipelineServiceImpl();

  @Before
  public void setup() {}

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutionStrategyList() {
    Map<ServiceDefinitionType, List<ExecutionStrategyType>> result = pipelineService.getExecutionStrategyList();
    assertThat(result.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStepsECS() {
    StepCategory result = pipelineService.getSteps(ServiceDefinitionType.ECS);
    assertThat(result.getName()).isEqualTo(PipelineServiceImpl.LIBRARY);
    assertThat(result.getStepsData()).isEqualTo(new ArrayList<>());
    assertThat(result.getStepCategories().size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStepsK8s() {
    StepCategory result = pipelineService.getSteps(ServiceDefinitionType.KUBERNETES);
    assertThat(result.getName()).isEqualTo(PipelineServiceImpl.LIBRARY);
    assertThat(result.getStepsData()).isEqualTo(new ArrayList<>());
    assertThat(result.getStepCategories().size()).isEqualTo(6);
  }
}