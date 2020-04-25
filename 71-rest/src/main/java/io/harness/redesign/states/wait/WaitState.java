package io.harness.redesign.states.wait;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.annotations.Redesign;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitate.modes.async.AsyncExecutable;
import io.harness.facilitate.modes.async.AsyncExecutableResponse;
import io.harness.state.State;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;
import io.harness.waiter.WaitNotifyEngine;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.WaitStateExecutionData;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.StateType;
import software.wings.sm.states.SimpleNotifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Redesign
public class WaitState implements State, AsyncExecutable {
  @Inject @Named("waitStateResumer") @Transient private ScheduledExecutorService executorService;
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    WaitStateParameters stateParameters = (WaitStateParameters) parameters;
    String resumeId = generateUuid();
    executorService.schedule(new SimpleNotifier(waitNotifyEngine, resumeId,
                                 ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build()),
        stateParameters.getWaitDurationSeconds(), TimeUnit.SECONDS);
    return AsyncExecutableResponse.builder().callbackId(resumeId).build();
  }

  @Override
  public StateResponse handleAsyncResponse(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> responseDataMap) {
    WaitStateParameters waitStateParameters = (WaitStateParameters) parameters;
    WaitStateExecutionData waitStateExecutionData = new WaitStateExecutionData();
    waitStateExecutionData.setDuration(waitStateParameters.getWaitDurationSeconds());
    return StateResponse.builder().status(NodeExecutionStatus.SUCCEEDED).output(waitStateExecutionData).build();
  }

  @Override
  public String getStateType() {
    return StateType.WAIT.name();
  }
}
