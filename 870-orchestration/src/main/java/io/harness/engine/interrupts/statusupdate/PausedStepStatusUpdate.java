package io.harness.engine.interrupts.statusupdate;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.execution.Status.PAUSED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.List;

@OwnedBy(CDC)
public class PausedStepStatusUpdate implements StepStatusUpdate {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    NodeExecution nodeExecution = nodeExecutionService.get(stepStatusUpdateInfo.getNodeExecutionId());
    if (nodeExecution.getParentId() == null) {
      planExecutionService.updateCalculatedStatus(stepStatusUpdateInfo.getPlanExecutionId());
      return;
    }
    List<NodeExecution> flowingChildren = nodeExecutionService.findByParentIdAndStatusIn(
        nodeExecution.getParentId(), StatusUtils.unpausableChildStatuses());
    if (isEmpty(flowingChildren)) {
      nodeExecutionService.updateStatusWithOps(nodeExecution.getParentId(), PAUSED, null, EnumSet.noneOf(Status.class));
    }
    planExecutionService.updateCalculatedStatus(stepStatusUpdateInfo.getPlanExecutionId());
  }
}
