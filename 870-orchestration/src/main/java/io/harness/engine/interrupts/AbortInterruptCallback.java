package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.helpers.AbortHelper;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;

@OwnedBy(PIPELINE)
public class AbortInterruptCallback implements OldNotifyCallback {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;
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
    abortHelper.abortDiscontinuingNode(nodeExecution, interruptId, interruptConfig);
    waitNotifyEngine.doneWith(nodeExecutionId + "|" + interruptId, response.values().iterator().next());
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {}
}
