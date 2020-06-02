package io.harness.facilitator.modes.task;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.tasks.Task;

import java.util.List;
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
@Redesign
public interface TaskExecutable {
  Task obtainTask(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  StepResponse handleTaskResult(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap);
}
