package io.harness.integrationstage;

import static io.harness.beans.environment.BuildJobEnvInfo.Type.K8;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CILiteEngineIntegrationStageModifierTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private CILiteEngineIntegrationStageModifier stageExecutionModifier;

  @Before
  public void setUp() {}

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
    assertThat(liteEngineTask.getSteps())
        .isEqualTo(ciExecutionPlanTestHelper.getExpectedExecutionElementWithoutCleanup());
    K8BuildJobEnvInfo envInfo = (K8BuildJobEnvInfo) liteEngineTask.getBuildJobEnvInfo();
    assertThat(envInfo.getType()).isEqualTo(K8);
    assertThat(envInfo.getWorkDir()).isEqualTo(stage.getWorkingDirectory());
    assertThat(envInfo.getPublishStepConnectorIdentifier())
        .isEqualTo(ciExecutionPlanTestHelper.getPublishArtifactConnectorIds());
    assertThat(envInfo.getPodsSetupInfo().getPodSetupInfoList().get(0).getPodSetupParams())
        .isEqualTo(
            ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod().getPodSetupInfoList().get(0).getPodSetupParams());
    assertThat(envInfo.getPodsSetupInfo().getPodSetupInfoList().get(0).getPvcParamsList().get(0).getVolumeName())
        .isEqualTo(ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod()
                       .getPodSetupInfoList()
                       .get(0)
                       .getPvcParamsList()
                       .get(0)
                       .getVolumeName());
  }
}