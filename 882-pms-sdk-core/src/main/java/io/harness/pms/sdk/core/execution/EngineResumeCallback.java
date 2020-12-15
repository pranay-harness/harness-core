package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class EngineResumeCallback implements NotifyCallback {
  @Inject PmsNodeExecutionService pmsNodeExecutionService;

  String nodeExecutionId;

  @Override
  public void notify(Map<String, ResponseData> response) {
    pmsNodeExecutionService.resumeNodeExecution(nodeExecutionId, response, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    pmsNodeExecutionService.resumeNodeExecution(nodeExecutionId, response, true);
  }
}
