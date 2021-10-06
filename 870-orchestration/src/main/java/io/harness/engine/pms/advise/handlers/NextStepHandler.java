package io.harness.engine.pms.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.pms.advise.AdviserResponseHandler;
import io.harness.execution.NodeExecution;
import io.harness.plan.Node;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.contracts.execution.Status;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.EnumSet;

@OwnedBy(CDC)
public class NextStepHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanService planService;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    NextStepAdvise advise = adviserResponse.getNextStepAdvise();
    if (advise.getToStatus() != Status.NO_OP) {
      nodeExecutionService.updateStatusWithOps(
          nodeExecution.getUuid(), advise.getToStatus(), null, EnumSet.noneOf(Status.class));
    }
    if (EmptyPredicate.isNotEmpty(advise.getNextNodeId())) {
      Node nextNode = Preconditions.checkNotNull(
          planService.fetchNode(nodeExecution.getAmbiance().getPlanId(), advise.getNextNodeId()));
      engine.triggerNode(nodeExecution.getAmbiance(), nextNode);
    } else {
      engine.endNodeExecution(nodeExecution.getAmbiance());
    }
  }
}
