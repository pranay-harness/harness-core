package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.ExecutionEngine;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
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
