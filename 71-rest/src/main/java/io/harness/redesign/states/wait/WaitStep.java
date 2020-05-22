package io.harness.redesign.states.wait;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.state.StateType;
import io.harness.state.Step;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.waiter.WaitNotifyEngine;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.WaitStateExecutionData;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.states.SimpleNotifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
@Redesign
@ExcludeRedesign
@Produces(Step.class)
public class WaitStep implements Step, AsyncExecutable {
  public static final StateType STATE_TYPE = StateType.builder().type("WAIT_STATE").build();

  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    WaitStepParameters parameters = (WaitStepParameters) stepParameters;
    String resumeId = generateUuid();
    executorService.schedule(new SimpleNotifier(waitNotifyEngine, resumeId,
                                 ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build()),
        parameters.getWaitDurationSeconds(), TimeUnit.SECONDS);
    return AsyncExecutableResponse.builder().callbackId(resumeId).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    WaitStepParameters parameters = (WaitStepParameters) stepParameters;
    WaitStateExecutionData waitStateExecutionData = new WaitStateExecutionData();
    waitStateExecutionData.setDuration(parameters.getWaitDurationSeconds());
    waitStateExecutionData.setWakeupTs(System.currentTimeMillis());
    return StepResponse.builder()
        .status(NodeExecutionStatus.SUCCEEDED)
        .outcome("waitData", waitStateExecutionData)
        .build();
  }

  @Override
  public StateType getType() {
    return STATE_TYPE;
  }
}
