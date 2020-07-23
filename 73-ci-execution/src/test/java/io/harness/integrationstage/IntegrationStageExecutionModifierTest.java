package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.StepSpecType;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class IntegrationStageExecutionModifierTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  public static final String POD_NAME = "testPod";
  private StageExecutionModifier stageExecutionModifier;

  @Before
  public void setUp() {
    stageExecutionModifier = IntegrationStageExecutionModifier.builder().podName(POD_NAME).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldModifyExecutionPlan() {
    IntegrationStage stage = ciExecutionPlanTestHelper.getIntegrationStage();
    ExecutionElement modifiedExecution = stageExecutionModifier.modifyExecutionPlan(stage.getExecution(), stage);
    assertThat(modifiedExecution).isNotNull();
    assertThat(modifiedExecution.getSteps()).isNotNull();

    ExecutionWrapper stepElementBuildEnv = modifiedExecution.getSteps().get(0);
    assertThat(stepElementBuildEnv).isInstanceOf(StepElement.class);
    StepSpecType stepSpecTypeBuildEnv = ((StepElement) stepElementBuildEnv).getStepSpecType();
    assertThat(stepSpecTypeBuildEnv).isInstanceOf(BuildEnvSetupStepInfo.class);

    ExecutionWrapper stepElementCleanup = modifiedExecution.getSteps().get(modifiedExecution.getSteps().size() - 1);
    assertThat(stepElementCleanup).isInstanceOf(StepElement.class);
    StepSpecType stepSpecTypeCleanup = ((StepElement) stepElementCleanup).getStepSpecType();
    assertThat(stepSpecTypeCleanup).isInstanceOf(CleanupStepInfo.class);
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testExpectingGitConnector() {
    IntegrationStage stage = IntegrationStage.builder()
                                 .execution(ciExecutionPlanTestHelper.getExecutionElement())
                                 .gitConnector(GitConnectorYaml.builder().type("***").build())
                                 .container(ciExecutionPlanTestHelper.getContainer())
                                 .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stageExecutionModifier.modifyExecutionPlan(stage.getExecution(), stage));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testExpectingGitConnectorWithoutGitStep() {
    IntegrationStage stage =
        IntegrationStage.builder()
            .execution(
                ExecutionElement.builder()
                    .steps(Arrays.asList(StepElement.builder().stepSpecType(RunStepInfo.builder().build()).build()))
                    .build())
            .gitConnector(ciExecutionPlanTestHelper.getConnector())
            .container(ciExecutionPlanTestHelper.getContainer())
            .build();

    assertThatExceptionOfType(InvalidRequestException.class)
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