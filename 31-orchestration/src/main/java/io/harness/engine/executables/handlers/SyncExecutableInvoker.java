package io.harness.engine.executables.handlers;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.facilitate.modes.sync.SyncExecutable;
import io.harness.state.io.StateResponse;
import io.harness.state.io.ambiance.Ambiance;
import lombok.extern.slf4j.Slf4j;

@Redesign
@Slf4j
public class SyncExecutableInvoker implements ExecutableInvoker {
  @Inject private ExecutionEngine engine;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    SyncExecutable syncExecutable = (SyncExecutable) invokerPackage.getState();
    Ambiance ambiance = invokerPackage.getAmbiance();
    StateResponse stateResponse = syncExecutable.executeSync(
        ambiance, invokerPackage.getParameters(), invokerPackage.getInputs(), invokerPackage.getPassThroughData());
    engine.handleStateResponse(ambiance.getCurrentRuntimeId(), stateResponse);
  }
}
