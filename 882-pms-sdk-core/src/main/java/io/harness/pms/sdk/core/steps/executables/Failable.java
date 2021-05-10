package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Map;

@OwnedBy(CDC)
public interface Failable<T extends StepParameters> {
  void handleFailureInterrupt(Ambiance ambiance, T stepParameters, Map<String, String> metadata);
}
