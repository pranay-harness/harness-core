package io.harness.engine.executables.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.io.StepResponse;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class SyncExecutableInvoker implements ExecutableInvoker {
  @Inject private ExecutionEngine engine;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    SyncExecutable syncExecutable = (SyncExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    StepResponse stepResponse = syncExecutable.executeSync(
        ambiance, invokerPackage.getParameters(), invokerPackage.getInputs(), invokerPackage.getPassThroughData());
    engine.handleStepResponse(ambiance.obtainCurrentRuntimeId(), stepResponse);
  }
}
