/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.integrationstage;

import static io.harness.common.CIExecutionConstants.STEP_WORK_DIR;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.FeatureName;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.VmBuildJobInfo;
import io.harness.beans.yaml.extended.infrastrucutre.VmInfraYaml;
import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.BuildJobEnvInfoBuilder;
import io.harness.ci.integrationstage.VmInitializeStepUtils;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.ff.CIFeatureFlagService;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class BuildJobEnvInfoBuilderTest extends CIExecutionTestBase {
  public static final String ACCOUNT_ID = "accountId";
  @Mock CIFeatureFlagService featureFlagService;
  @Inject VmBuildJobTestHelper vmBuildJobTestHelper;
  @Spy @InjectMocks private VmInitializeStepUtils vmInitializeStepUtils;
  @InjectMocks BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

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
    when(featureFlagService.isEnabled(FeatureName.CI_VM_INFRASTRUCTURE, "accountId")).thenReturn(true);
    StageElementConfig stageElementConfig = vmBuildJobTestHelper.getVmStage("test");
    Map<String, String> volToMountPath = new HashMap<>();
    volToMountPath.put("harness", "/harness");
    BuildJobEnvInfo expected = VmBuildJobInfo.builder()
                                   .workDir(STEP_WORK_DIR)
                                   .volToMountPath(volToMountPath)
                                   .connectorRefs(new ArrayList<>())
                                   .build();
    BuildJobEnvInfo actual = buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(
        stageElementConfig, VmInfraYaml.builder().build(), null, new ArrayList<>(), ACCOUNT_ID);
    assertThat(actual).isEqualTo(expected);
  }
}
