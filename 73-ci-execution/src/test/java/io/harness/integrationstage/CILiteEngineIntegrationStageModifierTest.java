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
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class CILiteEngineIntegrationStageModifierTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private CILiteEngineIntegrationStageModifier stageExecutionModifier;

  @Before
  public void setUp() {
    //    stageExecutionModifier =
    //        CILiteEngineIntegrationStageModifier.builder().podName(ciExecutionPlanTestHelper.getPodName()).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldModifyExecutionPlan() {
    IntegrationStage stage = ciExecutionPlanTestHelper.getIntegrationStage();
    ExecutionPlanCreationContextImpl executionPlanCreationContextWithExecutionArgs =
        ciExecutionPlanTestHelper.getExecutionPlanCreationContextWithExecutionArgs();
    ExecutionElement modifiedExecution = stageExecutionModifier.modifyExecutionPlan(
        stage.getExecution(), stage, executionPlanCreationContextWithExecutionArgs);
    assertThat(modifiedExecution).isNotNull();
    assertThat(modifiedExecution.getSteps()).isNotNull();
    StepElement step = (StepElement) modifiedExecution.getSteps().get(0);
    assertThat(step.getStepSpecType()).isInstanceOf(LiteEngineTaskStepInfo.class);
    LiteEngineTaskStepInfo liteEngineTask = (LiteEngineTaskStepInfo) step.getStepSpecType();
    assertThat(liteEngineTask).isInstanceOf(LiteEngineTaskStepInfo.class);
    assertThat(liteEngineTask.getBranchName()).isEqualTo("master");
    assertThat(liteEngineTask.getGitConnectorIdentifier()).isEqualTo("testGitConnector");
    assertThat(liteEngineTask.getSteps()).isEqualTo(ciExecutionPlanTestHelper.getExpectedExecutionElement());
    K8BuildJobEnvInfo envInfo = (K8BuildJobEnvInfo) liteEngineTask.getBuildJobEnvInfo();
    assertThat(envInfo.getType()).isEqualTo(K8);
    assertThat(envInfo.getWorkDir()).isEqualTo(stage.getWorkingDirectory());
    assertThat(envInfo.getPublishStepConnectorIdentifier())
        .isEqualTo(ciExecutionPlanTestHelper.getPublishArtifactConnectorIds());
    assertThat(envInfo.getPodsSetupInfo().getPodSetupInfoList().get(0).getPodSetupParams())
        .isEqualTo(
            ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod().getPodSetupInfoList().get(0).getPodSetupParams());
    assertThat(envInfo.getPodsSetupInfo().getPodSetupInfoList().get(0).getPvcParams().getVolumeName())
        .isEqualTo(ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod()
                       .getPodSetupInfoList()
                       .get(0)
                       .getPvcParams()
                       .getVolumeName());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testExpectingGitConnector() {
    ExecutionPlanCreationContextImpl executionPlanCreationContextWithExecutionArgs =
        ciExecutionPlanTestHelper.getExecutionPlanCreationContextWithExecutionArgs();
    IntegrationStage stage = IntegrationStage.builder()
                                 .execution(ciExecutionPlanTestHelper.getExecutionElement())
                                 .gitConnector(GitConnectorYaml.builder().type("***").build())
                                 .container(ciExecutionPlanTestHelper.getContainer())
                                 .build();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(()
                        -> stageExecutionModifier.modifyExecutionPlan(
                            stage.getExecution(), stage, executionPlanCreationContextWithExecutionArgs));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testExpectingGitConnectorWithoutGitStep() {
    ExecutionPlanCreationContextImpl executionPlanCreationContextWithExecutionArgs =
        ciExecutionPlanTestHelper.getExecutionPlanCreationContextWithExecutionArgs();
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
        .isThrownBy(()
                        -> stageExecutionModifier.modifyExecutionPlan(
                            stage.getExecution(), stage, executionPlanCreationContextWithExecutionArgs));
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testStageExecutionModifierBuilder() {
    IntegrationStageExecutionModifier modifier = IntegrationStageExecutionModifier.builder().build();
    assertThat(modifier).isNotNull();
    assertThat(modifier.hashCode()).isNotZero();
    assertThat(modifier.toString()).isNotEmpty();
  }
}