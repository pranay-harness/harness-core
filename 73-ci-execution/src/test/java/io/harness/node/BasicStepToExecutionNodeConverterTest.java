package io.harness.node;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graph.StepInfoGraph;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.plan.ExecutionNode;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

public class BasicStepToExecutionNodeConverterTest extends CIExecutionTest {
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

    ExecutionNode executionNode = basicStepToExecutionNodeConverter.convertStep(ciStepsGraph.getSteps().get(0),
        Collections.singletonList(ciStepsGraph.getSteps().get(1).getStepMetadata().getUuid()));

    assertThat(executionNode.getIdentifier()).isEqualTo(ENV_SETUP_NAME);
    assertThat(executionNode.getFacilitatorObtainments()).isNotEmpty();
    assertThat(executionNode.getAdviserObtainments()).isNotEmpty();
  }
}
