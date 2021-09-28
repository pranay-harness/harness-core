package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Value
@Builder
@Slf4j
public class AsyncSdkResumeCallback implements OldNotifyCallback {
  @Inject SdkNodeExecutionService sdkNodeExecutionService;

  String nodeExecutionId;
  String planExecutionId;
  Ambiance ambiance;

  @Override
  public void notify(Map<String, ResponseData> response) {
    // THis means new way of event got called and ambiance should be present
    if (EmptyPredicate.isEmpty(nodeExecutionId) && EmptyPredicate.isEmpty(planExecutionId)) {
      sdkNodeExecutionService.resumeNodeExecution(ambiance, response, false);
    }
    log.info("AsyncSdkResumeCallback notify is called for nodeExecutionId {}", nodeExecutionId);
    sdkNodeExecutionService.resumeNodeExecution(planExecutionId, nodeExecutionId, response, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    if (EmptyPredicate.isEmpty(nodeExecutionId) && EmptyPredicate.isEmpty(planExecutionId)) {
      sdkNodeExecutionService.resumeNodeExecution(ambiance, response, true);
    }
    log.info("AsyncSdkResumeCallback notifyError is called for nodeExecutionId {}", nodeExecutionId);
    sdkNodeExecutionService.resumeNodeExecution(planExecutionId, nodeExecutionId, response, true);
  }
}
