package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(PIPELINE)
public interface AsyncExecutableWithRbac<T extends StepParameters> extends AsyncExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  default AsyncExecutableResponse executeAsync(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData) {
    validateResources(ambiance, stepParameters);
    return this.executeAsyncAfterRbac(ambiance, stepParameters, inputPackage);
  }

  AsyncExecutableResponse executeAsyncAfterRbac(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
