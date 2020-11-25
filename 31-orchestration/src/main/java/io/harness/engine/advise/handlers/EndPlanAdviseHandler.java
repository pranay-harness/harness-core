package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.AmbianceUtils;
import io.harness.adviser.advise.EndPlanAdvise;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.ambiance.Ambiance;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class EndPlanAdviseHandler implements AdviseHandler<EndPlanAdvise> {
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void handleAdvise(Ambiance ambiance, EndPlanAdvise endPlanAdvise) {
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    engine.endTransition(nodeExecution);
  }
}
