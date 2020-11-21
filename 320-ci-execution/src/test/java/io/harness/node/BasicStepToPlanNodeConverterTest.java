package io.harness.node;

import static io.harness.rule.OwnerRule.HARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import graph.StepInfoGraph;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BasicStepToPlanNodeConverterTest extends CIExecutionTest {
  @Inject private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  private static final String ENV_SETUP_NAME = "envSetupName";

  @Before
  public void setUp() {}

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGenerateExecutionPlan() {
    StepInfoGraph ciStepsGraph = ciExecutionPlanTestHelper.getStepsGraph();

    PlanNode planNode = basicStepToExecutionNodeConverter.convertStep(
        ciStepsGraph.getSteps().get(0), Collections.singletonList(ciStepsGraph.getSteps().get(1).getIdentifier()));

    assertThat(planNode.getIdentifier()).isEqualTo(ENV_SETUP_NAME);
    assertThat(planNode.getFacilitatorObtainments()).isNotEmpty();
    assertThat(planNode.getAdviserObtainments()).isNotEmpty();
  }
}
