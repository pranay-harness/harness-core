package io.harness.plancreators;

import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.executionplan.CIExecutionPlanCreatorRegistrar;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.List;

public class CIPipelinePlanCreatorTest extends CIExecutionTest {
  @Inject private CIPipelinePlanCreator ciPipelinePlanCreator;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private CIExecutionPlanCreatorRegistrar ciExecutionPlanCreatorRegistrar;

  @Mock private PlanCreatorSearchContext<CDPipeline> planCreatorSearchContext;
  private CDPipelineEntity ciPipeline;
  @Before
  public void setUp() {
    ciExecutionPlanCreatorRegistrar.register();
    ciPipeline = ciExecutionPlanTestHelper.getCIPipeline();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPlan() {
    ExecutionPlanCreationContextImpl executionPlanCreationContextWithExecutionArgs =
        ciExecutionPlanTestHelper.getExecutionPlanCreationContextWithExecutionArgs();
    ExecutionPlanCreatorResponse plan =
        ciPipelinePlanCreator.createPlan(ciPipeline.getCdPipeline(), executionPlanCreationContextWithExecutionArgs);
    assertThat(plan.getPlanNodes()).isNotNull();
    List<PlanNode> planNodes = plan.getPlanNodes();
    assertThat(
        planNodes.stream().anyMatch(
            node -> "stages".equals(node.getIdentifier()) && "SECTION_CHAIN".equals(node.getStepType().getType())))
        .isTrue();
    assertThat(
        planNodes.stream().anyMatch(
            node -> "EXECUTION".equals(node.getIdentifier()) && "SECTION_CHAIN".equals(node.getStepType().getType())))
        .isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void supports() {
    when(planCreatorSearchContext.getObjectToPlan()).thenReturn(ciPipeline.getCdPipeline());
    when(planCreatorSearchContext.getType()).thenReturn(PIPELINE_PLAN_CREATOR.getName());
    assertThat(ciPipelinePlanCreator.supports(planCreatorSearchContext)).isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSupportedTypes() {
    assertThat(ciPipelinePlanCreator.getSupportedTypes()).contains(PIPELINE_PLAN_CREATOR.getName());
  }
}