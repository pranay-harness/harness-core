package io.harness.facilitate.modes.sync;

import io.harness.annotations.Redesign;
import io.harness.facilitate.PassThroughData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;

import java.util.List;

@Redesign
public interface SyncExecutable {
  StateResponse executeSync(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs, PassThroughData passThroughData);
}
