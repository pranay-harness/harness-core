package io.harness.engine.resume;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@Redesign
public class EngineResumeCallback implements NotifyCallback {
  @Inject ExecutionEngine executionEngine;

  String nodeInstanceId;

  @Override
  public void notify(Map<String, ResponseData> response) {
    executionEngine.resume(nodeInstanceId, response, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    executionEngine.resume(nodeInstanceId, response, true);
  }
}
