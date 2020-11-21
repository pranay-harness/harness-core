package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class EndPlanAdviseHandler implements AdviseHandler<EndPlanAdvise> {
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, EndPlanAdvise endPlanAdvise) {
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    engine.endTransition(nodeExecution);
  }
}
