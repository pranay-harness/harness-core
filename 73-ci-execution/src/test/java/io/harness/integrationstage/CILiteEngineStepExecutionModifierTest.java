package io.harness.integrationstage;

import static io.harness.beans.environment.BuildJobEnvInfo.Type.K8;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class CILiteEngineStepExecutionModifierTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  private StageExecutionModifier stageExecutionModifier;

  @Before
  public void setUp() {
    stageExecutionModifier =
        CILiteEngineStepExecutionModifier.builder().podName(ciExecutionPlanTestHelper.getPodName()).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldModifyExecutionPlan() {
    IntegrationStage stage = ciExecutionPlanTestHelper.getIntegrationStage();
    ExecutionElement modifiedExecution = stageExecutionModifier.modifyExecutionPlan(stage.getExecution(), stage);
    assertThat(modifiedExecution).isNotNull();
    assertThat(modifiedExecution.getSteps()).isNotNull();
    StepElement step = (StepElement) modifiedExecution.getSteps().get(0);
    assertThat(step.getStepSpecType()).isInstanceOf(LiteEngineTaskStepInfo.class);
    LiteEngineTaskStepInfo liteEngineTask = (LiteEngineTaskStepInfo) step.getStepSpecType();
    assertThat(liteEngineTask).isInstanceOf(LiteEngineTaskStepInfo.class);
    assertThat(liteEngineTask.getBranchName()).isEqualTo("master");
    assertThat(liteEngineTask.getGitConnectorIdentifier()).isEqualTo("testGitConnector");
    assertThat(liteEngineTask.getSteps()).isEqualTo(ciExecutionPlanTestHelper.getExecutionElement());
    K8BuildJobEnvInfo envInfo = (K8BuildJobEnvInfo) liteEngineTask.getBuildJobEnvInfo();
    assertThat(envInfo.getType()).isEqualTo(K8);
    assertThat(envInfo.getWorkDir()).isEqualTo(stage.getWorkingDirectory());
    assertThat(envInfo.getPublishStepConnectorIdentifier())
        .isEqualTo(ciExecutionPlanTestHelper.getPublishArtifactConnectorIds());
    assertThat(envInfo.getPodsSetupInfo()).isEqualTo(ciExecutionPlanTestHelper.getCIPodsSetupInfo());
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
    IntegrationStageExecutionModifier modifier =
        IntegrationStageExecutionModifier.builder().podName(ciExecutionPlanTestHelper.getPodName()).build();
    assertThat(modifier).isNotNull();
    assertThat(modifier.getPodName()).isEqualTo(ciExecutionPlanTestHelper.getPodName());
    assertThat(modifier.hashCode()).isNotZero();
    assertThat(modifier.toString()).isNotEmpty();
  }
}