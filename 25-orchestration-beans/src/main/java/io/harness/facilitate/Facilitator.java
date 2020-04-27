package io.harness.facilitate;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.state.io.StateTransput;

import java.util.List;

@Redesign
public interface Facilitator {
  FacilitatorType getType();

  FacilitatorResponse facilitate(Ambiance ambiance, List<StateTransput> inputs);
}
