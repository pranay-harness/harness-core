package io.harness.ci.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InitializeStepGeneratorTest extends CIExecutionTestBase {
  @Inject InitializeStepGenerator initializeStepGenerator;
  @Inject CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCreateLiteEngineTaskStepInfoFirstPod() {
    // input
    ExecutionElementConfig executionElementConfig = ciExecutionPlanTestHelper.getExecutionElementConfig();
    StageElementConfig stageElementConfig = ciExecutionPlanTestHelper.getIntegrationStageElementConfig();
    Infrastructure infrastructure = ciExecutionPlanTestHelper.getInfrastructure();
    String podName = "pod";
    Integer liteEngineCounter = 1;

    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    InitializeStepInfo actual = initializeStepGenerator.createLiteEngineTaskStepInfo(executionElementConfig,
        ciExecutionPlanTestHelper.getCICodebase(), stageElementConfig, ciExecutionArgs, infrastructure);
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().forEach(pvcParams -> pvcParams.setClaimName("")));

    InitializeStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod();
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().forEach(pvcParams -> pvcParams.setClaimName("")));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCreateLiteEngineTaskStepInfoOtherPod() {
    // input
    ExecutionElementConfig executionElementConfig = ciExecutionPlanTestHelper.getExecutionElementConfig();
    StageElementConfig stageElementConfig = ciExecutionPlanTestHelper.getIntegrationStageElementConfig();
    Infrastructure infrastructure = ciExecutionPlanTestHelper.getInfrastructure();

    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    InitializeStepInfo actual = initializeStepGenerator.createLiteEngineTaskStepInfo(
        executionElementConfig, null, stageElementConfig, ciExecutionArgs, infrastructure);
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    int index = 0;
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().forEach(pvcParams -> pvcParams.setClaimName("")));

    InitializeStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnOtherPods();
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().forEach(pvcParams -> pvcParams.setClaimName("")));

    assertThat(actual).isEqualTo(expected);
  }
}
