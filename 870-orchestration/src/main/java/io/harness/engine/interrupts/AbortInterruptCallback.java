package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.UnitStatus.FAILURE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.helpers.InterruptHelper;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
public class AbortInterruptCallback implements OldNotifyCallback {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  String nodeExecutionId;
  String interruptId;
  InterruptConfig interruptConfig;
  InterruptType interruptType;

  @Builder
  public AbortInterruptCallback(
      String nodeExecutionId, String interruptId, InterruptConfig interruptConfig, InterruptType interruptType) {
    this.nodeExecutionId = nodeExecutionId;
    this.interruptId = interruptId;
    this.interruptConfig = interruptConfig;
    this.interruptType = interruptType;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    List<UnitProgress> unitProgresses = InterruptHelper.evaluateUnitProgresses(nodeExecution, FAILURE);
    NodeExecution updatedNodeExecution =
        nodeExecutionService.updateStatusWithOps(nodeExecutionId, Status.ABORTED, ops -> {
          ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis());
          ops.set(NodeExecutionKeys.unitProgresses, unitProgresses);
          ops.addToSet(NodeExecutionKeys.interruptHistories,
              InterruptEffect.builder()
                  .interruptId(interruptId)
                  .tookEffectAt(System.currentTimeMillis())
                  .interruptType(interruptType)
                  .interruptConfig(interruptConfig)
                  .build());
        }, EnumSet.noneOf(Status.class));
    engine.endTransition(updatedNodeExecution);
    waitNotifyEngine.doneWith(nodeExecutionId + "|" + interruptId, response.values().iterator().next());
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {}
}
