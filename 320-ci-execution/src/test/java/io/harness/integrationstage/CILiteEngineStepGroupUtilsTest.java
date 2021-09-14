/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.CILiteEngineStepGroupUtils;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CILiteEngineStepGroupUtilsTest extends CIExecutionTestBase {
  //@Inject CILiteEngineStepGroupUtils ciLiteEngineStepGroupUtils;
  @Inject CILiteEngineStepGroupUtils ciExecutionPlanTestHelper;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createExecutionWrapperWithLiteEngineSteps() {
    //    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    //    List<ExecutionWrapper> executionWrapperWithLiteEngineSteps =
    //        ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
    //            ciExecutionPlanTestHelper.getIntegrationStage(), ciExecutionArgs, null, "accountId", "podName");
    //
    //    List<ExecutionWrapper> expectedExecutionWrapper = ciExecutionPlanTestHelper.getExpectedExecutionWrappers();
    //    expectedExecutionWrapper.addAll(ciExecutionPlanTestHelper.getExpectedExecutionElement(false).getSteps());
    //
    //    assertThat(executionWrapperWithLiteEngineSteps.get(0)).isInstanceOf(StepElement.class);
    //    StepElement stepElement = (StepElement) executionWrapperWithLiteEngineSteps.get(0);
    //    LiteEngineTaskStepInfo liteEngineTaskStepInfo = (LiteEngineTaskStepInfo) stepElement.getStepSpecType();
    //    ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .get(0)
    //        .getPvcParamsList()
    //        .get(0)
    //        .setClaimName("");
    //    ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .get(0)
    //        .setName("");
    //
    //    assertThat(executionWrapperWithLiteEngineSteps).isEqualTo(expectedExecutionWrapper);
  }
}
