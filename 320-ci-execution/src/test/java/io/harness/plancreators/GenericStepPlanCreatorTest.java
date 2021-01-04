package io.harness.plancreators;

import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STEP_PLAN_CREATOR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.plancreator.GenericStepPlanCreator;
import io.harness.rule.Owner;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class GenericStepPlanCreatorTest extends CIExecutionTest {
  @Inject GenericStepPlanCreator genericStepPlanCreator;

  @Mock ExecutionPlanCreationContext executionPlanCreationContext;
  @Mock PlanCreatorSearchContext<StepElement> planCreatorSearchContext;

  private GitCloneStepInfo stepInfo;

  @Before
  public void setUp() {
    stepInfo = GitCloneStepInfo.builder()
                   .identifier("testIdentifier")
                   .name("testName")

                   .branch("testBranch")
                   .gitConnector("testGitConnector")
                   .path("/test/path")

                   .retry(3)
                   .build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPlan() {
    StepElement stepElement = StepElement.builder().identifier("testIdentifier").stepSpecType(stepInfo).build();
    //    ExecutionPlanCreatorResponse plan = genericStepPlanCreator.createPlan(stepElement,
    //    executionPlanCreationContext); assertThat(plan.getPlanNodes()).isNotNull(); PlanNode planNode =
    //    plan.getPlanNodes().get(0); assertThat(planNode.getUuid()).isNotNull();
    //    assertThat(planNode.getName()).isEqualTo("testIdentifier");
    //    assertThat(planNode.getIdentifier()).isEqualTo(stepInfo.getIdentifier());
    //    assertThat(planNode.getStepType()).isEqualTo(stepElement.getType());
    //
    //    GitCloneStepInfo gotStepInfo = planNode.getStepParameters() == null
    //        ? null
    //        : JsonOrchestrationUtils.asObject(planNode.getStepParameters().toJson(), GitCloneStepInfo.class);
    //    assertThat(gotStepInfo).isNotNull();
    //    assertThat(gotStepInfo.getBranch()).isEqualTo("testBranch");
    //    assertThat(planNode.getFacilitatorObtainments().get(0).getType().getType())
    //        .isEqualTo(OrchestrationFacilitatorType.SYNC);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void supports() {
    StepElement stepElement = StepElement.builder().identifier("IDENTIFIER").stepSpecType(stepInfo).build();
    when(planCreatorSearchContext.getObjectToPlan()).thenReturn(stepElement);
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
