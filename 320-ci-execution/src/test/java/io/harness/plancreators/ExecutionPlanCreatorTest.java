package io.harness.plancreators;

import static io.harness.executionplan.CIPlanCreatorType.EXECUTION_PLAN_CREATOR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanCreatorRegistrar;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class ExecutionPlanCreatorTest extends CIExecutionTest {
  @Inject private ExecutionPlanCreator executionPlanCreator;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private CIExecutionPlanCreatorRegistrar ciExecutionPlanCreatorRegistrar;

  @Mock private ExecutionPlanCreationContext executionPlanCreationContext;
  @Mock private PlanCreatorSearchContext<ExecutionElement> planCreatorSearchContext;
  private ExecutionElement execution;
  @Before
  public void setUp() {
    ciExecutionPlanCreatorRegistrar.register();
    execution = ciExecutionPlanTestHelper.getExecutionElement();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPlan() {
    ExecutionPlanCreatorResponse plan = executionPlanCreator.createPlan(execution, executionPlanCreationContext);
    assertThat(plan.getPlanNodes()).isNotNull();
    List<PlanNode> planNodes = plan.getPlanNodes();
    assertThat(
        planNodes.stream().anyMatch(
            node -> "EXECUTION".equals(node.getIdentifier()) && "SECTION_CHAIN".equals(node.getStepType().getType())))
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
