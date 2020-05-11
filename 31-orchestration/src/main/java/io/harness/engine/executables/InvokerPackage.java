package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.state.State;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateTransput;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class InvokerPackage {
  State state;
  Ambiance ambiance;
  StateParameters parameters;
  List<StateTransput> inputs;
  PassThroughData passThroughData;
}
