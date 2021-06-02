package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;

@OwnedBy(PIPELINE)
public interface SyncExecutableWithRbac<T extends StepParameters> extends SyncExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @Override
  default StepResponse executeSync(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData) {
    validateResources(ambiance, stepParameters);
    return this.executeSyncAfterRbac(ambiance, stepParameters, inputPackage, passThroughData);
  }

  StepResponse executeSyncAfterRbac(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData);
}
