package io.harness.facilitator.modes;

import io.harness.ambiance.Ambiance;
import io.harness.state.io.StepParameters;

public interface Abortable<T extends ExecutableResponse> {
  void handleAbort(Ambiance ambiance, StepParameters stateParameters, T executableResponse);
}