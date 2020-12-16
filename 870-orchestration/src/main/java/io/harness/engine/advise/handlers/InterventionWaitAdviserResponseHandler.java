package io.harness.engine.advise.handlers;

import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;

import com.google.inject.Inject;

public class InterventionWaitAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEventEmitter eventEmitter;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, AdviserResponse adviserResponse) {
    // TODO(Garvit|Prashant) : What about TimeoutEngine Event ?
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .eventType(OrchestrationEventType.INTERVENTION_WAIT_START)
                               .ambiance(ambiance)
                               .nodeExecutionProto(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                               .build());

    nodeExecutionService.updateStatus(nodeExecutionId, Status.INTERVENTION_WAITING);
    planExecutionService.updateStatus(ambiance.getPlanExecutionId(), Status.INTERVENTION_WAITING);
  }
}
