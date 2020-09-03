package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.trackers.events.StatusUpdateTimeoutEvent;

import java.util.List;

@OwnedBy(CDC)
public class NodeExecutionStatusUpdateEventHandler implements OrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String nodeExecutionId = ambiance.obtainCurrentRuntimeId();
    if (nodeExecutionId == null) {
      return;
    }

    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    if (nodeExecution == null) {
      return;
    }

    List<String> timeoutInstanceIds = nodeExecution.getTimeoutInstanceIds();
    if (EmptyPredicate.isNotEmpty(timeoutInstanceIds)) {
      timeoutEngine.onEvent(timeoutInstanceIds, new StatusUpdateTimeoutEvent(nodeExecution.getStatus()));
    }
  }
}
