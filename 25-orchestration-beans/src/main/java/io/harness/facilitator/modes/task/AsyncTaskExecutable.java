package io.harness.facilitator.modes.task;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.tasks.Task;

import java.util.List;
import java.util.Map;

public interface AsyncTaskExecutable {
  Task obtainTask(Ambiance ambiance, StateParameters parameters, List<StepTransput> inputs);

  StepResponse handleTaskResult(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> responseDataMap);
}
