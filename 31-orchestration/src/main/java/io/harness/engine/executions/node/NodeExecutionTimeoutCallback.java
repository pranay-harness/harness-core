package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutDetails;
import io.harness.timeout.TimeoutInstance;

@OwnedBy(CDC)
public class NodeExecutionTimeoutCallback implements TimeoutCallback {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private InterruptManager interruptManager;

  private final String planExecutionId;
  private final String nodeExecutionId;

  public NodeExecutionTimeoutCallback(String planExecutionId, String nodeExecutionId) {
    this.planExecutionId = planExecutionId;
    this.nodeExecutionId = nodeExecutionId;
  }

  @Override
  public void onTimeout(TimeoutInstance timeoutInstance) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution == null || !Status.finalizableStatuses().contains(nodeExecution.getStatus())) {
      return;
    }

    nodeExecutionService.update(
        nodeExecutionId, ops -> ops.set(NodeExecutionKeys.timeoutDetails, new TimeoutDetails(timeoutInstance)));
    interruptManager.register(InterruptPackage.builder()
                                  .planExecutionId(planExecutionId)
                                  .nodeExecutionId(nodeExecutionId)
                                  .interruptType(ExecutionInterruptType.MARK_EXPIRED)
                                  .build());
  }
}
