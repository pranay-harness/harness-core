package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.Status;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
public class InterruptCallback implements NotifyCallback {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  String nodeExecutionId;
  String interruptId;
  Status finalStatus;

  @Builder
  public InterruptCallback(String nodeExecutionId, String interruptId, Status finalStatus) {
    this.nodeExecutionId = nodeExecutionId;
    this.interruptId = interruptId;
    this.finalStatus = finalStatus;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    NodeExecution updatedNodeExecution = nodeExecutionService.updateStatusWithOps(
        nodeExecutionId, finalStatus, ops -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis()));
    engine.endTransition(updatedNodeExecution, null);
    waitNotifyEngine.doneWith(nodeExecutionId + "|" + interruptId, response.values().iterator().next());
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {}
}
