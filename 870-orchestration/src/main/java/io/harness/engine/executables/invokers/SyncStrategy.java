package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.AmbianceUtils;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.plan.PlanNode;
import io.harness.pms.ambiance.Ambiance;
import io.harness.registries.state.StepRegistry;
import io.harness.state.io.StepResponse;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Redesign
@Slf4j
public class SyncStrategy implements ExecuteStrategy {
  @Inject private OrchestrationEngine engine;
  @Inject private StepRegistry stepRegistry;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    SyncExecutable syncExecutable = extractSyncExecutable(nodeExecution);
    StepResponse stepResponse =
        syncExecutable.executeSync(ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution),
            invokerPackage.getInputPackage(), invokerPackage.getPassThroughData());
    engine.handleStepResponse(AmbianceUtils.obtainCurrentRuntimeId(ambiance), stepResponse);
  }

  SyncExecutable extractSyncExecutable(NodeExecution nodeExecution) {
    PlanNode node = nodeExecution.getNode();
    return (SyncExecutable) stepRegistry.obtain(node.getStepType());
  }
}
