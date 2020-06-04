package io.harness.engine.resume;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;

import java.util.Map;

public class EngineResumeAllCallback implements NotifyCallback {
  @Inject ExecutionEngine executionEngine;

  Ambiance ambiance;

  @Builder
  public EngineResumeAllCallback(Ambiance ambiance) {
    this.ambiance = ambiance;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    executionEngine.startNodeExecution(ambiance);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // Do Nothing
  }
}
