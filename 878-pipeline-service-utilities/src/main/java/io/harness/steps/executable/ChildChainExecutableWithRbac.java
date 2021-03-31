package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildChainExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(PIPELINE)
public interface ChildChainExecutableWithRbac<T extends StepParameters> extends ChildChainExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @Override
  default ChildChainExecutableResponse executeFirstChild(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    validateResources(ambiance, stepParameters);
    return this.executeFirstChildAfterRbac(ambiance, stepParameters, inputPackage);
  }

  ChildChainExecutableResponse executeFirstChildAfterRbac(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
