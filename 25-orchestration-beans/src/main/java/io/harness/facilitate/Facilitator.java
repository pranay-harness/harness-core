package io.harness.facilitate;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateInput;
import io.harness.state.io.ambiance.Ambiance;

import java.util.List;

@Redesign
public interface Facilitator {
  FacilitatorType getType();

  FacilitatorResponse facilitate(Ambiance ambiance, List<StateInput> inputs);
}
