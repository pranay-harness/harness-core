package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(PIPELINE)
public interface TaskChainExecutableWithRbac<T extends StepParameters> extends TaskChainExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @Override
  default TaskChainResponse startChainLink(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    validateResources(ambiance, stepParameters);
    return this.startChainLinkAfterRbac(ambiance, stepParameters, inputPackage);
  }

  TaskChainResponse startChainLinkAfterRbac(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
