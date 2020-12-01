package io.harness.utils.steps;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.tasks.ResponseData;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.util.Map;

public class TestAsyncStep implements AsyncExecutable<TestStepParameters> {
  public static final StepType ASYNC_STEP_TYPE = StepType.newBuilder().setType("TEST_STATE_PLAN_ASYNC").build();

  @Inject private transient WaitNotifyEngine waitNotifyEngine;

  @Override
  public Class<TestStepParameters> getStepParametersClass() {
    return TestStepParameters.class;
  }

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
