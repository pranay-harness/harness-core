package io.harness.engine.interrupts.callback;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;

@OwnedBy(PIPELINE)
public class FailureInterruptCallback implements OldNotifyCallback {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private InterruptService interruptService;
  @Inject private OrchestrationEngine orchestrationEngine;

  String nodeExecutionId;
  String interruptId;
  InterruptConfig interruptConfig;
  InterruptType interruptType;

  @Builder
  public FailureInterruptCallback(
      String nodeExecutionId, String interruptId, InterruptConfig interruptConfig, InterruptType interruptType) {
    this.nodeExecutionId = nodeExecutionId;
    this.interruptId = interruptId;
    this.interruptConfig = interruptConfig;
    this.interruptType = interruptType;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    try {
      NodeExecution updatedNodeExecution = nodeExecutionService.update(nodeExecutionId, ops -> {
        ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis());
        ops.addToSet(NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(interruptId)
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(interruptType)
                .interruptConfig(interruptConfig)
                .build());
      });
      orchestrationEngine.concludeNodeExecution(updatedNodeExecution, Status.FAILED);
    } catch (Exception ex) {
      interruptService.markProcessed(interruptId, PROCESSED_UNSUCCESSFULLY);
      throw ex;
    }
    interruptService.markProcessed(interruptId, PROCESSED_SUCCESSFULLY);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {}
}
