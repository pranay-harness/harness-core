package io.harness.engine.advise.handlers;

import com.google.inject.Inject;

import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;

public class EndPlanAdviseHandler implements AdviseHandler<EndPlanAdvise> {
  @Inject private ExecutionEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, EndPlanAdvise endPlanAdvise) {
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    engine.endTransition(nodeExecution, nodeExecution.getStatus(), null);
  }
}
