package io.harness.utils.steps;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;

import java.util.Map;

public class TestAsyncStep implements Step, AsyncExecutable<TestStepParameters> {
  public static final StepType ASYNC_STEP_TYPE = StepType.builder().type("TEST_STATE_PLAN_ASYNC").build();

  @Inject private transient WaitNotifyEngine waitNotifyEngine;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, TestStepParameters stepParameters, StepInputPackage inputPackage) {
    String resumeId = generateUuid();
    waitNotifyEngine.doneWith(resumeId, StringNotifyResponseData.builder().data("SUCCESS").build());
    return AsyncExecutableResponse.builder().callbackId(resumeId).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, TestStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, TestStepParameters stateParameters, AsyncExecutableResponse executableResponse) {
    // Do Nothing
  }
}