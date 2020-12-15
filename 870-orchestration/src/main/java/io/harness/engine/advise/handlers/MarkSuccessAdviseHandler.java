package io.harness.engine.advise.handlers;

import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.MarkSuccessAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class MarkSuccessAdviseHandler implements AdviserResponseHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleAdvise(Ambiance ambiance, AdviserResponse adviserResponse) {
    MarkSuccessAdvise markSuccessAdvise = adviserResponse.getMarkSuccessAdvise();
    nodeExecutionService.updateStatus(AmbianceUtils.obtainCurrentRuntimeId(ambiance), Status.SUCCEEDED);
    PlanNodeProto nextNode = Preconditions.checkNotNull(
        planExecutionService.fetchExecutionNode(ambiance.getPlanExecutionId(), markSuccessAdvise.getNextNodeId()));
    engine.triggerExecution(ambiance, nextNode);
  }
}
