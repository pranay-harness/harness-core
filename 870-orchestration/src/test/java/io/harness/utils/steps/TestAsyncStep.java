package io.harness.utils.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(PIPELINE)
public class TestAsyncStep implements AsyncExecutable<TestStepParameters> {
  public static final StepType ASYNC_STEP_TYPE =
      StepType.newBuilder().setType("TEST_STATE_PLAN_ASYNC").setStepCategory(StepCategory.STEP).build();

  @Inject private transient WaitNotifyEngine waitNotifyEngine;

  @Override
  public Class<TestStepParameters> getStepParametersClass() {
    return TestStepParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(Ambiance ambiance, TestStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    String resumeId = generateUuid();
    waitNotifyEngine.doneWith(resumeId, StringNotifyResponseData.builder().data("SUCCESS").build());
    return AsyncExecutableResponse.newBuilder().addCallbackIds(resumeId).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, TestStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, TestStepParameters stepParameters, AsyncExecutableResponse executableResponse) {
    // Do Nothing
  }
}
