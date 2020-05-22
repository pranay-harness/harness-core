package io.harness.facilitator.modes.task;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.tasks.Task;

import java.util.List;
import java.util.Map;

public interface TaskWrapperExecutable {
  Task obtainTask(Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs);

  StateResponse handleTaskResult(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> responseDataMap);
}
