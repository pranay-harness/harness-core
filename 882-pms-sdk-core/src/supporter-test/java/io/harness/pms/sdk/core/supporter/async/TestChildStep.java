package io.harness.pms.sdk.core.supporter.async;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TestChildStep implements ChildExecutable<TestStepParameters> {
  public static final StepType CHILD_STEP_TYPE =
      StepType.newBuilder().setType("TEST_STATE_PLAN_CHILD").setStepCategory(StepCategory.STEP).build();

  @Override
  public Class<TestStepParameters> getStepParametersClass() {
    return TestStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, TestStepParameters stepParameters, StepInputPackage inputPackage) {
    return ChildExecutableResponse.newBuilder().build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, TestStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
