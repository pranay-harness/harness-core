package io.harness.engine.interrupts.statusupdate;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeExecutionUpdate {
  void onUpdate(OrchestrationEventType eventType, NodeExecution nodeExecution);
}
