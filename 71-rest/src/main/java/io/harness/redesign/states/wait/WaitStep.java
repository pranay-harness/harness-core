package io.harness.redesign.states.wait;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delay.SimpleNotifier;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.tasks.ResponseData;
import io.harness.waiter.WaitNotifyEngine;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.WaitStateExecutionData;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
@Redesign
public class WaitStep implements Step, AsyncExecutable<WaitStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("WAIT_STATE").build();

  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, WaitStepParameters waitStepParameters, StepInputPackage inputPackage) {
    String resumeId = generateUuid();
    executorService.schedule(new SimpleNotifier(waitNotifyEngine, resumeId,
                                 StatusNotifyResponseData.builder().status(Status.SUCCEEDED).build()),
        waitStepParameters.getWaitDurationSeconds(), TimeUnit.SECONDS);
    return AsyncExecutableResponse.builder().callbackId(resumeId).build();
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
