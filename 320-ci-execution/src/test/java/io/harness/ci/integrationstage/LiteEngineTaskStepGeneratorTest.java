/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
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

public class LiteEngineTaskStepGeneratorTest extends CIExecutionTestBase {
  @Inject LiteEngineTaskStepGenerator liteEngineTaskStepGenerator;
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
    boolean usePVC = true;

    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    LiteEngineTaskStepInfo actual = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(executionElementConfig,
        ciExecutionPlanTestHelper.getCICodebase(), stageElementConfig, ciExecutionArgs, podName, liteEngineCounter,
        usePVC, infrastructure);
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().get(0).setClaimName(""));

    LiteEngineTaskStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod();
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().get(0).setClaimName(""));

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

    Integer liteEngineCounter = 2;
    boolean usePVC = true;
    String podName = "pod";

    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    LiteEngineTaskStepInfo actual = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(executionElementConfig,
        null, stageElementConfig, ciExecutionArgs, podName, liteEngineCounter, usePVC, infrastructure);
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));

    LiteEngineTaskStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnOtherPods();
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));

    assertThat(actual).isEqualTo(expected);
  }
}
