package io.harness.state.io;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class StateExecutionPackage {
  Ambiance ambiance;
  List<StateTransput> inputs;
  StateParameters stateParameter;
}
