package io.harness.pms.sdk.core.steps;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(CDC)
public interface Step<T extends StepParameters> {
  Class<T> getStepParametersClass();
}
