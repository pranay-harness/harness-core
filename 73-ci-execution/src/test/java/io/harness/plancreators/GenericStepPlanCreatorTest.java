package io.harness.plancreators;

import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.steps.AbstractStepWithMetaInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class GenericStepPlanCreatorTest extends CIExecutionTest {
  @Inject GenericStepPlanCreator genericStepPlanCreator;

  @Mock CreateExecutionPlanContext createExecutionPlanContext;
  @Mock PlanCreatorSearchContext<AbstractStepWithMetaInfo> planCreatorSearchContext;

  private GitCloneStepInfo stepInfo;

  @Before
  public void setUp() {
    stepInfo = GitCloneStepInfo.builder()
                   .branch("testBranch")
                   .identifier("testIdentifier")
                   .gitConnector("testGitConnector")
                   .path("/test/path")
                   .name("testName")
                   .retry(3)
                   .timeout(60)
                   .build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPlan() {
    CreateExecutionPlanResponse plan = genericStepPlanCreator.createPlan(stepInfo, createExecutionPlanContext);
    assertThat(plan.getPlanNodes()).isNotNull();
    PlanNode planNode = plan.getPlanNodes().get(0);
    assertThat(planNode.getUuid()).isEqualTo(stepInfo.getStepMetadata().getUuid());
    assertThat(planNode.getName()).isEqualTo(stepInfo.getStepMetadata().getUuid());
    assertThat(planNode.getIdentifier()).isEqualTo(stepInfo.getIdentifier());
    assertThat(planNode.getStepType()).isEqualTo(stepInfo.getNonYamlInfo().getStepType());
    assertThat(planNode.getStepParameters()).isEqualTo(stepInfo);
    assertThat(planNode.getFacilitatorObtainments().get(0).getType().getType()).isEqualTo(FacilitatorType.SYNC);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void supports() {
    when(planCreatorSearchContext.getObjectToPlan()).thenReturn(stepInfo);
    when(planCreatorSearchContext.getType()).thenReturn(STEP_PLAN_CREATOR.getName());
    assertThat(genericStepPlanCreator.supports(planCreatorSearchContext)).isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getSupportedTypes() {
    assertThat(genericStepPlanCreator.getSupportedTypes()).contains(STEP_PLAN_CREATOR.getName());
  }
}