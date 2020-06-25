package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.OrchestrationEngine;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;

import java.util.Map;

@OwnedBy(CDC)
public class EngineResumeAllCallback implements NotifyCallback {
  @Inject OrchestrationEngine orchestrationEngine;

  String nodeExecutionId;

  @Builder
  public EngineResumeAllCallback(String nodeExecutionId) {
    this.nodeExecutionId = nodeExecutionId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    orchestrationEngine.startNodeExecution(nodeExecutionId);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // Do Nothing
  }
}
