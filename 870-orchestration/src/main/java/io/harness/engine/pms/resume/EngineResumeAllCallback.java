package io.harness.engine.pms.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(CDC)
public class EngineResumeAllCallback implements OldNotifyCallback {
  @Inject OrchestrationEngine orchestrationEngine;

  @Deprecated @Getter String nodeExecutionId;
  @Getter Ambiance ambiance;

  @Builder
  public EngineResumeAllCallback(Ambiance ambiance, String nodeExecutionId) {
    this.ambiance = ambiance;
    this.nodeExecutionId = nodeExecutionId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    // This is for backward compatibility will clear in next release
    if (isNotEmpty(nodeExecutionId)) {
      orchestrationEngine.startNodeExecution(nodeExecutionId);
      return;
    }
    orchestrationEngine.startNodeExecution(ambiance);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // Do Nothing
  }
}
