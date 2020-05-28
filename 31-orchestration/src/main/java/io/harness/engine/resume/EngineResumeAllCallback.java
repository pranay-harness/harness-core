package io.harness.engine.resume;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.state.io.StepTransput;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;

import java.util.List;
import java.util.Map;

public class EngineResumeAllCallback implements NotifyCallback {
  @Inject ExecutionEngine executionEngine;

  Ambiance ambiance;
  List<StepTransput> additionalInputs;

  @Builder
  public EngineResumeAllCallback(Ambiance ambiance, List<StepTransput> additionalInputs) {
    this.ambiance = ambiance;
    this.additionalInputs = additionalInputs;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    executionEngine.startNodeExecution(ambiance, additionalInputs);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // Do Nothing
  }
}
