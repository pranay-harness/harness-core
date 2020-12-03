package io.harness.redesign.states.wait;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delay.SimpleNotifier;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.AsyncExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.api.WaitStateExecutionData;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mongodb.morphia.annotations.Transient;

@OwnedBy(CDC)
@Redesign
public class WaitStep implements AsyncExecutable<WaitStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("WAIT_STATE").build();

  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public Class<WaitStepParameters> getStepParametersClass() {
    return WaitStepParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, WaitStepParameters waitStepParameters, StepInputPackage inputPackage) {
    String resumeId = generateUuid();
    executorService.schedule(new SimpleNotifier(waitNotifyEngine, resumeId,
                                 StatusNotifyResponseData.builder().status(Status.SUCCEEDED).build()),
        waitStepParameters.getWaitDurationSeconds(), TimeUnit.SECONDS);
    return AsyncExecutableResponse.newBuilder().addCallbackIds(resumeId).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, WaitStepParameters waitStepParameters, Map<String, ResponseData> responseDataMap) {
    WaitStateExecutionData waitStateExecutionData = new WaitStateExecutionData();
    waitStateExecutionData.setDuration(waitStepParameters.getWaitDurationSeconds());
    waitStateExecutionData.setWakeupTs(System.currentTimeMillis());
    waitStateExecutionData.setStatus(ExecutionStatus.SUCCESS);
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder().name("waitData").outcome(waitStateExecutionData).build())
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, WaitStepParameters stateParameters, AsyncExecutableResponse executableResponse) {
    // TODO : Handle Abort
  }
}
