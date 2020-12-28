package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.BuildJobEnvInfoBuilder;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BuildJobEnvInfoBuilderTest extends CIExecutionTest {
  //@Inject BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;
  @Inject BuildJobEnvInfoBuilder ciExecutionPlanTestHelper;

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
}
