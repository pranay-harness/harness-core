package io.harness.engine.advise.handlers;

import io.harness.AmbianceUtils;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;

import com.google.inject.Inject;

public class InterventionWaitAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, AdviserResponse adviserResponse) {
    // TODO(Garvit|Prashant) : What about TimeoutEngine Event ?
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .eventType(OrchestrationEventType.INTERVENTION_WAIT_START)
                               .ambiance(ambiance)
                               .build());

    nodeExecutionService.updateStatus(AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.INTERVENTION_WAITING);
    planExecutionService.updateStatus(ambiance.getPlanExecutionId(), Status.INTERVENTION_WAITING);
  }
}
