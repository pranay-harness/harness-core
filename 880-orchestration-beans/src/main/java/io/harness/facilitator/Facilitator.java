package io.harness.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.state.io.StepInputPackage;

@OwnedBy(CDC)
@Redesign
public interface Facilitator {
  FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage);
}
