package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
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

  @Deprecated String nodeExecutionId;
  @Deprecated String planExecutionId;
  byte[] ambianceBytes;

  @Override
  public void notify(Map<String, ResponseData> response) {
    // THis means new way of event got called and ambiance should be present
    notifyWithError(response, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    notifyWithError(response, true);
  }

  private void notifyWithError(Map<String, ResponseData> response, boolean asyncError) {
    if (EmptyPredicate.isEmpty(nodeExecutionId) && EmptyPredicate.isEmpty(planExecutionId)) {
      try {
        Ambiance ambiance = Ambiance.parseFrom(ambianceBytes);
        log.info("AsyncSdkResumeCallback notify is called for ambiance with nodeExecutionId {}",
            AmbianceUtils.obtainCurrentRuntimeId(ambiance));
        sdkNodeExecutionService.resumeNodeExecution(ambiance, response, asyncError);
      } catch (InvalidProtocolBufferException e) {
        log.error("Not able to deserialize Ambiance bytes. Progress Callback will not be executed");
      }
      return;
    }
    log.info("AsyncSdkResumeCallback notify is called for nodeExecutionId {}", nodeExecutionId);
    sdkNodeExecutionService.resumeNodeExecution(planExecutionId, nodeExecutionId, response, asyncError);
  }
}
