package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.interrupts.AdviserIssuer;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.interrupts.IssuedBy;
import io.harness.pms.contracts.interrupts.RetryInterruptConfig;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class EngineWaitRetryCallback implements OldNotifyCallback {
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
    interruptManager.register(
        InterruptPackage.builder()
            .planExecutionId(planExecutionId)
            .nodeExecutionId(nodeExecutionId)
            .interruptType(InterruptType.RETRY)
            .interruptConfig(
                InterruptConfig.newBuilder()
                    .setIssuedBy(
                        IssuedBy.newBuilder()
                            .setAdviserIssuer(AdviserIssuer.newBuilder().setAdviserType(AdviseType.RETRY).build())
                            .build())
                    .setRetryInterruptConfig(RetryInterruptConfig.newBuilder().build())
                    .build())
            .build());
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    log.info("Retry Error Callback Received");
  }
}
