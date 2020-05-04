package io.harness.facilitator;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.registries.RegistrableEntity;
import io.harness.state.io.StateTransput;

import java.util.List;

@Redesign
public interface Facilitator extends RegistrableEntity<FacilitatorType> {
  FacilitatorType getType();

  FacilitatorResponse facilitate(Ambiance ambiance, FacilitatorParameters parameters, List<StateTransput> inputs);
}
