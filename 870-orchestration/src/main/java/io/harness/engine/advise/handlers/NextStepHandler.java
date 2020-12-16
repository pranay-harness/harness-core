package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.contracts.plan.PlanNodeProto;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@OwnedBy(CDC)
public class NextStepHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEngine engine;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    NextStepAdvise advise = adviserResponse.getNextStepAdvise();
    PlanNodeProto nextNode = Preconditions.checkNotNull(planExecutionService.fetchExecutionNode(
        nodeExecution.getAmbiance().getPlanExecutionId(), advise.getNextNodeId()));
    engine.triggerExecution(nodeExecution.getAmbiance(), nextNode);
  }
}
