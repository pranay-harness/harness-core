package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.AmbianceUtils;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.ambiance.Ambiance;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class EndPlanAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, AdviserResponse adviserResponse) {
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    engine.endTransition(nodeExecution);
  }
}
