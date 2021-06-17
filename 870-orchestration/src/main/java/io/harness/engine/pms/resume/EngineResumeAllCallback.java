package io.harness.engine.pms.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
public class EngineResumeAllCallback implements OldNotifyCallback {
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
