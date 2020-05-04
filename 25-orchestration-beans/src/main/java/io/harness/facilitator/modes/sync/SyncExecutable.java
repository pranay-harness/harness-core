package io.harness.facilitator.modes.sync;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.facilitator.PassThroughData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;

import java.util.List;

@Redesign
public interface SyncExecutable {
  StateResponse executeSync(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs, PassThroughData passThroughData);
}
