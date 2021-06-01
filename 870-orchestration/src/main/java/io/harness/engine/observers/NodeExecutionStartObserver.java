package io.harness.engine.observers;

import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;

public interface NodeExecutionStartObserver {
  void onNodeStart(OrchestrationEventType eventType, NodeExecution nodeExecution);
}
