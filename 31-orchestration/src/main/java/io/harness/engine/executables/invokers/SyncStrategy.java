package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.InvokeStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.io.StepResponse;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class SyncStrategy implements InvokeStrategy {
  @Inject private OrchestrationEngine engine;

  @Override
  public void invoke(InvokerPackage invokerPackage) {
    SyncExecutable syncExecutable = (SyncExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    StepResponse stepResponse = syncExecutable.executeSync(ambiance, invokerPackage.getParameters(),
        invokerPackage.getInputPackage(), invokerPackage.getPassThroughData());
    engine.handleStepResponse(ambiance.obtainCurrentRuntimeId(), stepResponse);
  }
}
