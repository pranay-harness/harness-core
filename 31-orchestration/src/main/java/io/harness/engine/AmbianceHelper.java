package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;

@OwnedBy(CDC)
@Redesign
public class AmbianceHelper {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;

  public NodeExecution obtainNodeExecution(Ambiance ambiance) {
    String nodeExecutionId = ambiance == null ? null : ambiance.obtainCurrentRuntimeId();
    if (nodeExecutionId == null) {
      return null;
    }
    return nodeExecutionService.get(nodeExecutionId);
  }

  public PlanExecution obtainPlanExecution(Ambiance ambiance) {
    String planExecutionId = ambiance == null ? null : ambiance.getPlanExecutionId();
    if (planExecutionId == null) {
      return null;
    }
    return planExecutionService.get(planExecutionId);
  }

  public Ambiance fetchAmbiance(NodeExecution nodeExecution) {
    if (nodeExecution == null) {
      return null;
    }

    PlanExecution planExecution = planExecutionService.get(nodeExecution.getPlanExecutionId());
    Preconditions.checkNotNull(planExecution);
    return Ambiance.fromNodeExecution(planExecution.getInputArgs(), nodeExecution);
  }

  public Ambiance fetchAmbianceForRetry(NodeExecution nodeExecution) {
    Ambiance ambiance = fetchAmbiance(nodeExecution);
    return ambiance.cloneForFinish();
  }
}
