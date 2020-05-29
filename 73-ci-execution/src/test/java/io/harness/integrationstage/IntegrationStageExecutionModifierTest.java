package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.yaml.core.Execution;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;

public class IntegrationStageExecutionModifierTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  public static final String POD_NAME = "testPod";
  private StageExecutionModifier stageExecutionModifier;

  @Before
  public void setUp() throws Exception {
    stageExecutionModifier = IntegrationStageExecutionModifier.builder().podName(POD_NAME).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldModifyExecutionPlan() {
    IntegrationStage stage = ciExecutionPlanTestHelper.getIntegrationStage();
    Execution modifiedExecution = stageExecutionModifier.modifyExecutionPlan(stage.getExecution(), stage);
    assertThat(modifiedExecution).isNotNull();
    assertThat(modifiedExecution.getSteps()).isNotNull();
    assertThat(modifiedExecution.getSteps().get(0)).isInstanceOf(BuildEnvSetupStepInfo.class);
    assertThat(modifiedExecution.getSteps().get(modifiedExecution.getSteps().size() - 1))
        .isInstanceOf(CleanupStepInfo.class);
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testExpectingGitConnector() {
    IntegrationStage stage = IntegrationStage.builder()
                                 .execution(ciExecutionPlanTestHelper.getExecution())
                                 .connector(GitConnectorYaml.builder().type("***").build())
                                 .container(ciExecutionPlanTestHelper.getContainer())
                                 .artifact(ciExecutionPlanTestHelper.getArtifact())
                                 .build();

    Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stageExecutionModifier.modifyExecutionPlan(stage.getExecution(), stage));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testExpectingGitConnectorWithoutGitStep() {
    IntegrationStage stage =
        IntegrationStage.builder()
            .execution(Execution.builder().steps(new ArrayList<>(Arrays.asList(RunStepInfo.builder().build()))).build())
            .connector(ciExecutionPlanTestHelper.getConnector())
            .container(ciExecutionPlanTestHelper.getContainer())
            .artifact(ciExecutionPlanTestHelper.getArtifact())
            .build();

    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> stageExecutionModifier.modifyExecutionPlan(stage.getExecution(), stage));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testStageExecutionModifierBuilder() {
    IntegrationStageExecutionModifier modifier = IntegrationStageExecutionModifier.builder().podName(POD_NAME).build();
    assertThat(modifier).isNotNull();
    assertThat(modifier.getPodName()).isEqualTo(POD_NAME);
    assertThat(modifier.hashCode()).isNotZero();
    assertThat(modifier.toString()).isNotEmpty();
  }
}