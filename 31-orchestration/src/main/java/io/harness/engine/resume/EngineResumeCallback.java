package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class EngineResumeCallback implements NotifyCallback {
  @Inject OrchestrationEngine orchestrationEngine;

  String nodeExecutionId;

  @Override
  public void notify(Map<String, ResponseData> response) {
    orchestrationEngine.resume(nodeExecutionId, response, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    orchestrationEngine.resume(nodeExecutionId, response, true);
  }
}
