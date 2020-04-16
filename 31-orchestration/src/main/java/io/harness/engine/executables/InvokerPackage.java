package io.harness.engine.executables;

import io.harness.annotations.Redesign;
import io.harness.facilitate.PassThroughData;
import io.harness.state.State;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;
import lombok.Builder;
import lombok.Value;

import java.util.List;

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
