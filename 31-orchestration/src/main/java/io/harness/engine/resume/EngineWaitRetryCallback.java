package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class EngineWaitRetryCallback implements NotifyCallback {
  @Inject private InterruptManager interruptManager;

  @NonNull String planExecutionId;
  @NonNull String nodeExecutionId;

  @Builder
  public EngineWaitRetryCallback(@NonNull String planExecutionId, @NonNull String nodeExecutionId) {
    this.planExecutionId = planExecutionId;
    this.nodeExecutionId = nodeExecutionId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    interruptManager.register(InterruptPackage.builder()
                                  .planExecutionId(planExecutionId)
                                  .nodeExecutionId(nodeExecutionId)
                                  .interruptType(ExecutionInterruptType.RETRY)
                                  .build());
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info("Retry Error Callback Received");
  }
}
