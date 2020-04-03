package io.harness.facilitate.modes.sync;

import io.harness.annotations.Redesign;
import io.harness.facilitate.PassThroughData;
import io.harness.state.io.StateExecutionPackage;
import io.harness.state.io.StateResponse;

@Redesign
public interface SyncExecutable {
  StateResponse executeSync(StateExecutionPackage stateExecutionPackage, PassThroughData passThroughData);
}
