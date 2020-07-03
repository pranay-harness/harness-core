package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;

@OwnedBy(CDC)
@Redesign
public interface Facilitator extends RegistrableEntity {
  FacilitatorResponse facilitate(Ambiance ambiance, StepParameters stepParameters, FacilitatorParameters parameters,
      StepInputPackage inputPackage);
}
