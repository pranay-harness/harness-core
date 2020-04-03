package io.harness.state.io;

import io.harness.annotations.Redesign;
import io.harness.state.io.ambiance.Ambiance;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class StateExecutionPackage {
  Ambiance ambiance;
  List<StateInput> inputs;
  StateParameters stateParameter;
}
