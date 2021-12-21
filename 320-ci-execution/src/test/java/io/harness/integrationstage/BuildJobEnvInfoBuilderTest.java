package io.harness.integrationstage;

import static io.harness.common.CIExecutionConstants.STEP_WORK_DIR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.VmBuildJobInfo;
import io.harness.beans.yaml.extended.infrastrucutre.VmInfraYaml;
import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.BuildJobEnvInfoBuilder;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BuildJobEnvInfoBuilderTest extends CIExecutionTestBase {
  @Inject BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;
  @Inject VmBuildJobTestHelper vmBuildJobTestHelper;

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
  public void getVmBuildJobEnvInfo() {
    StageElementConfig stageElementConfig = vmBuildJobTestHelper.getVmStage("test");
    Map<String, String> volToMountPath = new HashMap<>();
    volToMountPath.put("harness", "/harness");
    BuildJobEnvInfo expected = VmBuildJobInfo.builder()
                                   .workDir(STEP_WORK_DIR)
                                   .volToMountPath(volToMountPath)
                                   .connectorRefs(new ArrayList<>())
                                   .build();
    BuildJobEnvInfo actual = buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(
        stageElementConfig, VmInfraYaml.builder().build(), null, new ArrayList<>(), null);
    assertThat(actual).isEqualTo(expected);
  }
}
