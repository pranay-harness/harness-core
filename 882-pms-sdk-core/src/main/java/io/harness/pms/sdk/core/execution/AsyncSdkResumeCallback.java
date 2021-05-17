package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class AsyncSdkResumeCallback implements OldNotifyCallback {
  @Inject SdkNodeExecutionService sdkNodeExecutionService;

  String nodeExecutionId;

  @Override
  public void notify(Map<String, ResponseData> response) {
    sdkNodeExecutionService.resumeNodeExecution(nodeExecutionId, response, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    sdkNodeExecutionService.resumeNodeExecution(nodeExecutionId, response, true);
  }
}
