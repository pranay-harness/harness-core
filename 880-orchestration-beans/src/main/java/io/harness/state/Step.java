package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(CDC)
@Redesign
public interface Step<T extends StepParameters> {
  Class<T> getStepParametersClass();
}
