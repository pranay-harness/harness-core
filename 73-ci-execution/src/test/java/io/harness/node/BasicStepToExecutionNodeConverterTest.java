package io.harness.node;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graph.CIStepsGraph;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.plan.ExecutionNode;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class BasicStepToExecutionNodeConverterTest extends CIExecutionTest {
  @Inject private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  private static final String ENV_SETUP_NAME = "envSetupName";

  @Before
  public void setUp() {}

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGenerateExecutionPlan() throws IOException {
    CIStepsGraph ciStepsGraph = ciExecutionPlanTestHelper.getStepsGraph();

    ExecutionNode executionNode = basicStepToExecutionNodeConverter.convertStep(
        ciStepsGraph.getCiSteps().get(0), ciStepsGraph.getCiSteps().get(1).getCiStepMetadata().getUuid());

    assertThat(executionNode.getName()).isEqualTo(ENV_SETUP_NAME);
    assertThat(executionNode.getFacilitatorObtainments()).isNotEmpty();
    assertThat(executionNode.getAdviserObtainments()).isNotEmpty();
  }
}
