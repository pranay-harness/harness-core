package io.harness.engine.advise.handlers;

import com.google.inject.Inject;

import io.harness.adviser.advise.InterventionWaitAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.status.Status;

public class InterventionWaitAdviseHandler implements AdviseHandler<InterventionWaitAdvise> {
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, InterventionWaitAdvise endPlanAdvise) {
    // TODO(Garvit|Prashant) : What about TimeoutEngine Event ?
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .eventType(OrchestrationEventType.INTERVENTION_WAIT_START)
                               .ambiance(ambiance)
                               .build());
    nodeExecutionService.updateStatus(ambiance.obtainCurrentRuntimeId(), Status.INTERVENTION_WAITING);
  }
}
