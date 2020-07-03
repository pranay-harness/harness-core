package io.harness.facilitator.modes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StepParameters;

@OwnedBy(CDC)
@Redesign
public interface Abortable<T extends ExecutableResponse> {
  void handleAbort(Ambiance ambiance, StepParameters stateParameters, T executableResponse);
}