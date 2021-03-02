package io.harness.pms.sdk.core.steps.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import java.util.Map;

/**
 * Use this interface when your task spawns a task. The queuing of the task will be taken care by the framework itself.
 * For this interface the abort etc. for task will also be handled by the framework
 *
 * InterfaceDefinition:
 *
 * obtainTask: In return we expect a {@link Task}. It can spawn any task which conforms to the defined interface.
 *
 * handleTaskResult : The result of the task will be supplied in the responseDataMap. The key will be the waitId, See
 * {@link Task} for details on this
 */
@OwnedBy(CDC)
public interface TaskExecutable<T extends StepParameters> extends Step<T>, Abortable<T, TaskExecutableResponse> {
  TaskRequest obtainTask(Ambiance ambiance, T stepParameters, StepInputPackage inputPackage);

  StepResponse handleTaskResult(Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap);

  default void handleAbort(Ambiance ambiance, T stepParameters, TaskExecutableResponse executableResponse) {
    // NOOP : By default this is noop as task abortion is handled by the PMS but you are free to override it
  }
}
