package io.harness.plancreators;

import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.steps.StepInfoType;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanCreatorRegistrar;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.yaml.core.Execution;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.List;

public class ExecutionPlanCreatorTest extends CIExecutionTest {
  @Inject private ExecutionPlanCreator executionPlanCreator;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private CIExecutionPlanCreatorRegistrar ciExecutionPlanCreatorRegistrar;

  @Mock private CreateExecutionPlanContext createExecutionPlanContext;
  @Mock private PlanCreatorSearchContext<Execution> planCreatorSearchContext;
  private Execution execution;
  @Before
  public void setUp() {
    ciExecutionPlanCreatorRegistrar.register();
    execution = ciExecutionPlanTestHelper.getExecution();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPlan() {
    CreateExecutionPlanResponse plan = executionPlanCreator.createPlan(execution, createExecutionPlanContext);
    assertThat(plan.getPlanNodes()).isNotNull();
    List<PlanNode> planNodes = plan.getPlanNodes();
    assertThat(
        planNodes.stream().anyMatch(
            node -> "EXECUTION".equals(node.getIdentifier()) && "SECTION_CHAIN".equals(node.getStepType().getType())))
        .isTrue();
    assertThat(planNodes.stream().anyMatch(node
                   -> "git-1".equals(node.getIdentifier())
                       && StepInfoType.GIT_CLONE.name().equals(node.getStepType().getType())))
        .isTrue();
    assertThat(planNodes.stream().anyMatch(node
                   -> "git-2".equals(node.getIdentifier())
                       && StepInfoType.GIT_CLONE.name().equals(node.getStepType().getType())))
        .isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void supports() {
    when(planCreatorSearchContext.getObjectToPlan()).thenReturn(execution);
    when(planCreatorSearchContext.getType()).thenReturn(EXECUTION_PLAN_CREATOR.getName());
    assertThat(executionPlanCreator.supports(planCreatorSearchContext)).isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSupportedTypes() {
    assertThat(executionPlanCreator.getSupportedTypes()).contains(EXECUTION_PLAN_CREATOR.getName());
  }
}