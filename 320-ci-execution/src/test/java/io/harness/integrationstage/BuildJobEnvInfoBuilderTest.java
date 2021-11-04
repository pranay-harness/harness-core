package io.harness.integrationstage;

import static io.harness.common.CIExecutionConstants.STEP_WORK_DIR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.environment.AwsVmBuildJobInfo;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.BuildJobEnvInfoBuilder;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BuildJobEnvInfoBuilderTest extends CIExecutionTestBase {
  @Inject BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;
  @Inject AwsVmBuildJobTestHelper awsVmBuildJobTestHelper;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getCIBuildJobEnvInfo() {
    //    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    //    K8BuildJobEnvInfo actual = (K8BuildJobEnvInfo) buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(
    //        ciExecutionPlanTestHelper.getIntegrationStage(), ciExecutionArgs,
    //        ciExecutionPlanTestHelper.getExpectedExecutionSectionsWithLESteps(false), true, "buildnumber22850");
    //    actual.getPodsSetupInfo().getPodSetupInfoList().forEach(podSetupInfo -> podSetupInfo.setName(""));
    //    actual.getPodsSetupInfo().getPodSetupInfoList().forEach(
    //        podSetupInfo -> podSetupInfo.getPvcParamsList().get(0).setClaimName(""));
    //
    //    BuildJobEnvInfo expected = ciExecutionPlanTestHelper.getCIBuildJobEnvInfoOnFirstPod();
    //
    //    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getAwsVmBuildJobEnvInfo() {
    StageElementConfig stageElementConfig = awsVmBuildJobTestHelper.getAwsVmStage("test");

    BuildJobEnvInfo expected = AwsVmBuildJobInfo.builder().workDir(STEP_WORK_DIR).build();
    BuildJobEnvInfo actual = buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(stageElementConfig, null, null);
    assertThat(actual).isEqualTo(expected);
  }
}
