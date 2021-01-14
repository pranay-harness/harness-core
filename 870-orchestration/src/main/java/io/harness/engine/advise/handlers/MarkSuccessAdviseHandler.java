package io.harness.engine.advise.handlers;

import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.MarkSuccessAdvise;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class MarkSuccessAdviseHandler implements AdviserResponseHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    MarkSuccessAdvise markSuccessAdvise = adviserResponse.getMarkSuccessAdvise();
    nodeExecutionService.updateStatus(nodeExecution.getUuid(), Status.SUCCEEDED);
    if (EmptyPredicate.isNotEmpty(markSuccessAdvise.getNextNodeId())) {
      PlanNodeProto nextNode = Preconditions.checkNotNull(planExecutionService.fetchExecutionNode(
          nodeExecution.getAmbiance().getPlanExecutionId(), markSuccessAdvise.getNextNodeId()));
      engine.triggerExecution(nodeExecution.getAmbiance(), nextNode);
    } else {
      engine.endNodeExecution(nodeExecution.getUuid(), Status.SUCCEEDED);
    }
  }
}
