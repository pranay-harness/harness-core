package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.tasks.ResponseData;

@OwnedBy(PIPELINE)
public interface TaskExecutableWithRbac<T extends StepParameters, R extends ResponseData> extends TaskExecutable<T, R> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @Override
  default TaskRequest obtainTask(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage) {
    validateResources(ambiance, stepParameters);
    return this.obtainTaskAfterRbac(ambiance, stepParameters, inputPackage);
  }

  TaskRequest obtainTaskAfterRbac(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);
}
