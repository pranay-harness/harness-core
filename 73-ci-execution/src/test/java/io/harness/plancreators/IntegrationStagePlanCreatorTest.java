package io.harness.plancreators;

import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGE_PLAN_CREATOR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.stages.IntegrationStage;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanCreatorRegistrar;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.List;

public class IntegrationStagePlanCreatorTest extends CIExecutionTest {
  @Inject private IntegrationStagePlanCreator integrationStagePlanCreator;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private CIExecutionPlanCreatorRegistrar ciExecutionPlanCreatorRegistrar;

  @Mock private CreateExecutionPlanContext createExecutionPlanContext;
  @Mock private PlanCreatorSearchContext<IntegrationStage> planCreatorSearchContext;
  private IntegrationStage integrationStage;
  @Before
  public void setUp() {
    ciExecutionPlanCreatorRegistrar.register();
    integrationStage = ciExecutionPlanTestHelper.getIntegrationStage();
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPlan() {
    CreateExecutionPlanResponse plan =
        integrationStagePlanCreator.createPlan(integrationStage, createExecutionPlanContext);
    assertThat(plan.getPlanNodes()).isNotNull();
    List<PlanNode> planNodes = plan.getPlanNodes();
    assertThat(planNodes.get(0).getIdentifier()).isEqualTo(integrationStage.getIdentifier());
    assertThat(
        planNodes.stream().anyMatch(
            node -> "EXECUTION".equals(node.getIdentifier()) && "SECTION_CHAIN".equals(node.getStepType().getType())))
        .isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void supports() {
    when(planCreatorSearchContext.getObjectToPlan()).thenReturn(integrationStage);
    when(planCreatorSearchContext.getType()).thenReturn(STAGE_PLAN_CREATOR.getName());
    assertThat(integrationStagePlanCreator.supports(planCreatorSearchContext)).isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSupportedTypes() {
    assertThat(integrationStagePlanCreator.getSupportedTypes()).contains(STAGE_PLAN_CREATOR.getName());
  }
}