package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(CDC)
public interface Abortable<T extends StepParameters, V> {
  void handleAbort(Ambiance ambiance, T stateParameters, V executableResponse);
}
